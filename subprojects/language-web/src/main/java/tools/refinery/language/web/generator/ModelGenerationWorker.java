/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.generator;

import com.google.inject.Inject;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.generator.ModelGenerator;
import tools.refinery.generator.ModelGeneratorFactory;
import tools.refinery.language.web.semantics.metadata.MetadataCreator;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.generator.ValidationErrorsException;
import tools.refinery.language.web.semantics.PartialInterpretation2Json;
import tools.refinery.language.web.xtext.server.ThreadPoolExecutorServiceProvider;
import tools.refinery.language.web.xtext.server.push.PushWebDocument;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.util.CancellationToken;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;

public class ModelGenerationWorker implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(ModelGenerationWorker.class);

	private final UUID uuid = UUID.randomUUID();

	private PushWebDocument state;

	private String text;

	private volatile boolean timedOut;

	private volatile boolean cancelled;

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private ProblemLoader problemLoader;

	@Inject
	private ModelGeneratorFactory generatorFactory;

	@Inject
	private MetadataCreator metadataCreator;

	@Inject
	private PartialInterpretation2Json partialInterpretation2Json;

	private final Object lockObject = new Object();

	private ExecutorService executorService;

	private ScheduledExecutorService scheduledExecutorService;

	private int randomSeed;

	private long timeoutSec;

	private Future<?> future;

	private ScheduledFuture<?> timeoutFuture;

	private final CancellationToken cancellationToken = () -> {
		if (cancelled || Thread.interrupted()) {
			operationCanceledManager.throwOperationCanceledException();
		}
	};

	@Inject
	public void setExecutorServiceProvider(ThreadPoolExecutorServiceProvider provider) {
		executorService = provider.get(ModelGenerationService.MODEL_GENERATION_EXECUTOR);
		scheduledExecutorService = provider.getScheduled(ModelGenerationService.MODEL_GENERATION_TIMEOUT_EXECUTOR);
	}

	public void setState(PushWebDocument state, int randomSeed, long timeoutSec) {
		this.state = state;
		this.randomSeed = randomSeed;
		this.timeoutSec = timeoutSec;
		text = state.getText();
	}

	public UUID getUuid() {
		return uuid;
	}

	public void start() {
		synchronized (lockObject) {
			LOG.debug("Enqueueing model generation: {}", uuid);
			future = executorService.submit(this);
		}
	}

	public void startTimeout() {
		synchronized (lockObject) {
			LOG.debug("Starting model generation: {}", uuid);
			cancellationToken.checkCancelled();
			timeoutFuture = scheduledExecutorService.schedule(() -> cancel(true), timeoutSec, TimeUnit.SECONDS);
		}
	}

	// We catch {@code Throwable} to handle {@code OperationCancelledError}, but we rethrow fatal JVM errors.
	@SuppressWarnings("squid:S1181")
	@Override
	public void run() {
		startTimeout();
		notifyResult(new ModelGenerationStatusResult(uuid, "Initializing model generator"));
		ModelGenerationResult result;
		try {
			result = doRun();
		} catch (Throwable e) {
			if (operationCanceledManager.isOperationCanceledException(e)) {
				var message = timedOut ? "Model generation timed out" : "Model generation cancelled";
				LOG.debug("{}: {}", message, uuid);
				notifyResult(new ModelGenerationErrorResult(uuid, message));
			} else if (e instanceof Error error) {
				// Make sure we don't try to recover from any fatal JVM errors.
				throw error;
			} else {
				LOG.debug("Model generation error", e);
				notifyResult(new ModelGenerationErrorResult(uuid, e.toString()));
			}
			return;
		}
		notifyResult(result);
	}

	private void notifyResult(ModelGenerationResult result) {
		state.notifyPrecomputationListeners(ModelGenerationService.SERVICE_NAME, result);
	}

	public ModelGenerationResult doRun() throws IOException {
		cancellationToken.checkCancelled();
		var problem = problemLoader.cancellationToken(cancellationToken).loadString(text);
		ModelGenerator generator;
		try {
			generator = generatorFactory.cancellationToken(cancellationToken).createGenerator(problem);
		} catch (ValidationErrorsException e) {
			var errors = e.getErrors();
			if (errors != null && !errors.isEmpty()) {
				return new ModelGenerationErrorResult(uuid, "Validation error: " + errors.getFirst().getMessage());
			}
			throw e;
		}
		notifyResult(new ModelGenerationStatusResult(uuid, "Generating model"));
		generator.setRandomSeed(randomSeed);
		if (!generator.tryGenerate()) {
			return new ModelGenerationErrorResult(uuid, "Problem is unsatisfiable");
		}
		notifyResult(new ModelGenerationStatusResult(uuid, "Saving generated model"));
		cancellationToken.checkCancelled();
		metadataCreator.setProblemTrace(generator.getProblemTrace());
		var nodesMetadata = metadataCreator.getNodesMetadata(generator.getModel(), Concreteness.CANDIDATE);
		cancellationToken.checkCancelled();
		var relationsMetadata = metadataCreator.getRelationsMetadata();
		cancellationToken.checkCancelled();
		var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(generator, cancellationToken);
		return new ModelGenerationSuccessResult(uuid, nodesMetadata, relationsMetadata, partialInterpretation);
	}

	public void cancel() {
		cancel(false);
	}

	public void cancel(boolean timedOut) {
		synchronized (lockObject) {
			LOG.trace("Cancelling model generation: {}", uuid);
			this.timedOut = timedOut;
			cancelled = true;
			if (future != null) {
				future.cancel(true);
				future = null;
			}
			if (timeoutFuture != null) {
				timeoutFuture.cancel(true);
				timeoutFuture = null;
			}
		}
	}
}
