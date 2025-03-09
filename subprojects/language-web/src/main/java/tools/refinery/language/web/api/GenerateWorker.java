/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api;

import com.google.inject.Inject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.generator.*;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.web.api.dto.GenerateRequest;
import tools.refinery.language.web.api.dto.GenerateStatus;
import tools.refinery.language.web.api.dto.GenerateSuccessResult;
import tools.refinery.language.web.api.dto.RefineryResponse;
import tools.refinery.language.web.api.sink.ResponseSink;
import tools.refinery.language.web.api.util.OutputSerializer;
import tools.refinery.language.web.api.util.TimeoutManager;
import tools.refinery.language.web.xtext.server.ThreadPoolExecutorServiceProvider;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GenerateWorker extends ScheduledWorker<GenerateRequest> {
	private static final Logger LOG = LoggerFactory.getLogger(GenerateWorker.class);

	@Inject
	private TimeoutManager timeoutManager;

	@Inject
	private ProblemLoader problemLoader;

	@Inject
	private ModelGeneratorFactory modelGeneratorFactory;

	@Inject
	private OutputSerializer outputSerializer;

	@Override
	protected String getExecutorServiceKey() {
		return ThreadPoolExecutorServiceProvider.MODEL_GENERATION_EXECUTOR;
	}

	@Override
	protected Duration getTimeout() {
		return timeoutManager.getModelGenerationTimeout();
	}

	@Override
	protected void initialize(GenerateRequest request, ResponseSink responseSink) {
		super.initialize(request, responseSink);
		var cancellationToken = getCancellationToken();
		problemLoader.cancellationToken(cancellationToken);
		modelGeneratorFactory.cancellationToken(cancellationToken);
		outputSerializer.setCancellationToken(cancellationToken);
	}

	@Override
	protected void run() throws IOException {
		updateStatusString("Initializing model generator");
		var problem = loadProblem();
		if (problem == null) {
			return;
		}
		var generator = createModelGenerator(problem);
		updateStatusString("Generating model");
		generator.generate();
		updateStatusString("Saving generated model");
		saveModel(generator);
	}

	private void updateStatusString(String status) {
		updateStatus(new GenerateStatus(status));
	}

	private @Nullable Problem loadProblem() throws IOException {
		var request = getRequest();
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
		try {
			return problemLoader.loadScopeConstraints(originalProblem, scopeConstraints, overrideScopeConstraints);
		} catch (InvalidScopeConstraintException | InvalidProblemException e) {
			setInvalidScopeConstraintsError(e);
			return null;
		}
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

	private ModelGenerator createModelGenerator(Problem problem) {
		checkCancelled();
		var request = getRequest();
		var jsonFormat = request.getFormat().getJson();
		modelGeneratorFactory.keepNonExistingObjects(jsonFormat.getNonExistingObjects().isKeep());
		modelGeneratorFactory.keepShadowPredicates(jsonFormat.getShadowPredicates().isKeep());
		var generator = modelGeneratorFactory.createGenerator(problem);
		generator.setRandomSeed(request.getRandomSeed());
		generator.setMaxNumberOfSolutions(1);
		return generator;
	}


	private void saveModel(ModelGenerator generator) throws IOException {
		var request = getRequest();
		boolean jsonEnabled = request.getFormat().getJson().isEnabled();
		var json = jsonEnabled ? outputSerializer.savePartialInterpretation(generator) : null;
		boolean sourceEnabled = request.getFormat().getSource().isEnabled();
		var source = sourceEnabled ? outputSerializer.saveSource(generator) : null;
		setResponse(new RefineryResponse.Success(new GenerateSuccessResult(json, source)));
	}
}
