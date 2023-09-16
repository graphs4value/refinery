/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.generator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.LazyStringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.language.semantics.model.ModelInitializer;
import tools.refinery.language.web.semantics.PartialInterpretation2Json;
import tools.refinery.language.web.xtext.server.ThreadPoolExecutorServiceProvider;
import tools.refinery.language.web.xtext.server.push.PushWebDocument;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.util.CancellationToken;

import java.io.IOException;
import java.util.Map;
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
	private Provider<XtextResourceSet> resourceSetProvider;

	@Inject
	private IResourceFactory resourceFactory;

	@Inject
	private IResourceValidator resourceValidator;

	@Inject
	private ModelInitializer initializer;

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
		var resourceSet = resourceSetProvider.get();
		var uri = URI.createURI("__synthetic_" + uuid + ".problem");
		var resource = resourceFactory.createResource(uri);
		resourceSet.getResources().add(resource);
		var inputStream = new LazyStringInputStream(text);
		resource.load(inputStream, Map.of());
		cancellationToken.checkCancelled();
		var issues = resourceValidator.validate(resource, CheckMode.ALL, () -> cancelled || Thread.interrupted());
		cancellationToken.checkCancelled();
		for (var issue : issues) {
			if (issue.getSeverity() == Severity.ERROR) {
				return new ModelGenerationErrorResult(uuid, "Validation error: " + issue.getMessage());
			}
		}
		if (resource.getContents().isEmpty() || !(resource.getContents().get(0) instanceof Problem problem)) {
			return new ModelGenerationErrorResult(uuid, "Model generation problem not found");
		}
		cancellationToken.checkCancelled();
		var storeBuilder = ModelStore.builder()
				.cancellationToken(cancellationToken)
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(Concreteness.CANDIDATE));
		var modelSeed = initializer.createModel(problem, storeBuilder);
		var store = storeBuilder.build();
		cancellationToken.checkCancelled();
		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var initialVersion = model.commit();
		cancellationToken.checkCancelled();
		notifyResult(new ModelGenerationStatusResult(uuid, "Generating model"));
		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion, randomSeed);
		cancellationToken.checkCancelled();
		var solutionStore = bestFirst.getSolutionStore();
		if (solutionStore.getSolutions().isEmpty()) {
			return new ModelGenerationErrorResult(uuid, "Problem is unsatisfiable");
		}
		notifyResult(new ModelGenerationStatusResult(uuid, "Saving generated model"));
		model.restore(solutionStore.getSolutions().get(0).version());
		cancellationToken.checkCancelled();
		metadataCreator.setInitializer(initializer);
		var nodesMetadata = metadataCreator.getNodesMetadata(model.getAdapter(ReasoningAdapter.class).getNodeCount(),
				false);
		cancellationToken.checkCancelled();
		var relationsMetadata = metadataCreator.getRelationsMetadata();
		cancellationToken.checkCancelled();
		var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(initializer, model,
				Concreteness.CANDIDATE, cancellationToken);
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
