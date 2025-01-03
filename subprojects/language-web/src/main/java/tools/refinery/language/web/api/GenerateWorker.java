/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api;

import com.google.inject.Inject;
import org.eclipse.xtext.service.OperationCanceledError;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.generator.*;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.web.api.dto.GenerateRequest;
import tools.refinery.language.web.api.dto.GenerateSuccessResult;
import tools.refinery.language.web.api.dto.JsonOutput;
import tools.refinery.language.web.api.dto.RefineryResponse;
import tools.refinery.language.web.api.util.ResponseSink;
import tools.refinery.language.web.api.util.ServerExceptionMapper;
import tools.refinery.language.web.generator.ModelGenerationService;
import tools.refinery.language.web.semantics.PartialInterpretation2Json;
import tools.refinery.language.web.semantics.SemanticsService;
import tools.refinery.language.web.xtext.server.ThreadPoolExecutorServiceProvider;
import tools.refinery.store.util.CancellationToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GenerateWorker implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(GenerateWorker.class);
	private static final long TIMEOUT_SEC =
			SemanticsService.getTimeout("REFINERY_MODEL_GENERATION_TIMEOUT_SEC").orElse(600L);

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private ProblemLoader problemLoader;

	@Inject
	private ModelGeneratorFactory modelGeneratorFactory;

	@Inject
	private PartialInterpretation2Json partialInterpretation2Json;

	@Inject
	private ServerExceptionMapper serverExceptionMapper;

	private GenerateRequest request;
	private ResponseSink responseSink;
	private CancellationToken cancellationToken;
	private ScheduledExecutorService scheduledExecutorService;
	private ScheduledFuture<?> timeoutFuture;
	private volatile boolean timedOut;

	@Inject
	public void setExecutorServiceProvider(ThreadPoolExecutorServiceProvider provider) {
		scheduledExecutorService = provider.getScheduled(ModelGenerationService.MODEL_GENERATION_TIMEOUT_EXECUTOR);
	}

	public void initialize(GenerateRequest request, ResponseSink responseSink) {
		this.request = request;
		this.responseSink = responseSink;
		cancellationToken = () -> {
			if (timedOut || responseSink.isCancelled() || Thread.interrupted()) {
				operationCanceledManager.throwOperationCanceledException();
			}
		};
		problemLoader.cancellationToken(cancellationToken);
		modelGeneratorFactory.cancellationToken(cancellationToken);
	}

	@Override
	public void run() {
		try {
			scheduleTimeout();
			doRun();
		} catch (RuntimeException e) {
			// Catch all exceptions here, because we might not get joined to the webserver thread in an asynchronous
			// API call.
			LOG.error("Unhandled exception during model generation", e);
		}
	}

	private void scheduleTimeout() {
		timeoutFuture = scheduledExecutorService.schedule(() -> {
			try {
				LOG.debug("Model generation timed out");
				timedOut = true;
				setResponse(RefineryResponse.Timeout.of());
			} catch (RuntimeException e) {
				LOG.error("Error sending timeout response", e);
			}
		}, TIMEOUT_SEC, TimeUnit.SECONDS);
	}

	private void doRun() {
		try {
			generateModel();
		} catch (OperationCanceledError | Exception e) {
			try (var response = serverExceptionMapper.toResponse(e)) {
				setResponse(response.getStatus(), (RefineryResponse) response.getEntity());
			}
		} finally {
			timeoutFuture.cancel(true);
		}
	}

	private void setResponse(RefineryResponse response) {
		responseSink.setResponse(response);
	}

	private void setResponse(int statusCode, RefineryResponse response) {
		responseSink.setResponse(statusCode, response);
	}

	private void updateStatus(String status) {
		checkCancelled();
		LOG.debug("Status: {}", status);
		responseSink.updateStatus(status);
	}

	private void checkCancelled() {
		cancellationToken.checkCancelled();
	}

	private void generateModel() throws IOException {
		updateStatus("Initializing model generator");
		Problem problem;
		try {
			problem = loadProblem();
		} catch (InvalidScopeConstraintException | ValidationErrorsException e) {
			setInvalidScopeConstraintsError(e);
			return;
		}
		var generator = createModelGenerator(problem);
		if (generator == null) {
			return;
		}
		updateStatus("Generating model");
		if (generator.tryGenerate() != GeneratorResult.SUCCESS) {
			LOG.debug("Problem is unsatisfiable");
			setResponse(new RefineryResponse.Unsatisfiable("Problem is unsatisfiable"));
			return;
		}
		updateStatus("Saving generated model");
		saveModel(generator);
	}

	private Problem loadProblem() throws IOException {
		var originalProblem = problemLoader.loadString(request.getInput().getSource());
		var scopeConstraints = new ArrayList<String>();
		var overrideScopeConstraints = new ArrayList<String>();
		for (var scope : request.getScopes()) {
			var scopeConstraint = scope.toScopeConstraint();
			if (scope.isOverride()) {
				overrideScopeConstraints.add(scopeConstraint);
			} else {
				scopeConstraints.add(scopeConstraint);
			}
		}
		return problemLoader.loadScopeConstraints(originalProblem, scopeConstraints, overrideScopeConstraints);
	}

	private void setInvalidScopeConstraintsError(Throwable t) {
		var message = "Invalid scope constraints";
		LOG.debug(message, t);
		var exceptionMessage = t.getMessage();
		var summary = exceptionMessage == null ? message : message + ": " + exceptionMessage;
		setResponse(new RefineryResponse.RequestError(summary, List.of(
				new RefineryResponse.RequestError.Detail("$.scopes", message)
		)));
	}

	private @Nullable ModelGenerator createModelGenerator(Problem problem) {
		checkCancelled();
		var jsonFormat = request.getFormat().getJson();
		modelGeneratorFactory.keepNonExistingObjects(jsonFormat.getNonExistingObjects().isKeep());
		modelGeneratorFactory.keepShadowPredicates(jsonFormat.getShadowPredicates().isKeep());
		var generator = modelGeneratorFactory.tryCreateGenerator(problem);
		if (generator.getInitializationResult() instanceof ModelFacadeResult.Rejected rejectedResult) {
			var message = rejectedResult.formatMessage();
			LOG.debug("Propagation rejected: {}", message);
			setResponse(new RefineryResponse.Unsatisfiable("Problem is unsatisfiable: " + message));
			return null;
		}
		generator.setRandomSeed(request.getRandomSeed());
		generator.setMaxNumberOfSolutions(1);
		return generator;
	}


	private void saveModel(ModelGenerator generator) throws IOException {
		boolean jsonEnabled = request.getFormat().getJson().isEnabled();
		var json = jsonEnabled ? savePartialInterpretation(generator) : null;
		boolean sourceEnabled = request.getFormat().getSource().isEnabled();
		var source = sourceEnabled ? saveSource(generator) : null;
		setResponse(new RefineryResponse.Success(new GenerateSuccessResult(json, source)));
	}

	private String saveSource(ModelGenerator generator) throws IOException {
		checkCancelled();
		var serializedSolution = generator.serialize();
		checkCancelled();
		try (var outputStream = new ByteArrayOutputStream()) {
			serializedSolution.eResource().save(outputStream, Map.of());
			return outputStream.toString(StandardCharsets.UTF_8);
		}
	}

	private JsonOutput savePartialInterpretation(ModelGenerator generator) {
		checkCancelled();
		var nodes = generator.getNodesMetadata().list();
		checkCancelled();
		var relations = generator.getRelationsMetadata();
		checkCancelled();
		var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(generator, cancellationToken);
		return new JsonOutput(nodes, relations, partialInterpretation);
	}
}
