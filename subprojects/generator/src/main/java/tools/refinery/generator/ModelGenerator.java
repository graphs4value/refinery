/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Provider;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.SolutionSerializer;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.statespace.SolutionStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ModelGenerator extends ModelFacade {
	private final Version initialVersion;
	private final Provider<SolutionSerializer> solutionSerializerProvider;
	private final CancellableCancellationToken cancellationToken;
	private final boolean keepNonExistingObjects;
	private final PartialInterpretation<TruthValue, Boolean> existsInterpretation;
	private long randomSeed = 1;
	private int maxNumberOfSolutions = 1;
	private SolutionStore solutionStore;

	ModelGenerator(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed,
				   Provider<SolutionSerializer> solutionSerializerProvider,
				   CancellableCancellationToken cancellationToken, boolean keepNonExistingObjects) {
		super(problemTrace, store, modelSeed, Concreteness.CANDIDATE);
		this.solutionSerializerProvider = solutionSerializerProvider;
		this.cancellationToken = cancellationToken;
		this.keepNonExistingObjects = keepNonExistingObjects;
		existsInterpretation = super.getPartialInterpretation(ReasoningAdapter.EXISTS_SYMBOL);
		initialVersion = getModel().commit();
	}

	public long getRandomSeed() {
		return randomSeed;
	}

	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
		this.solutionStore = null;
	}

	public int getMaxNumberOfSolutions() {
		return maxNumberOfSolutions;
	}

	public void setMaxNumberOfSolutions(int maxNumberOfSolutions) {
		this.maxNumberOfSolutions = maxNumberOfSolutions;
		this.solutionStore = null;
	}

	public int getSolutionCount() {
		if (!isLastGenerationSuccessful()) {
			return 0;
		}
		return this.solutionStore.getSolutions().size();
	}

	public void loadSolution(int index) {
		if (index >= getSolutionCount()) {
			throw new IndexOutOfBoundsException("No such solution");
		}
		getModel().restore(solutionStore.getSolutions().get(index).version());
	}

	// It makes more sense to check for success than for failure.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isLastGenerationSuccessful() {
		return solutionStore != null;
	}

	public GeneratorResult tryGenerate() {
		if (cancellationToken.isCancelled()) {
			throw new IllegalStateException("Model generation was previously cancelled");
		}
		solutionStore = null;
		randomSeed++;
		var bestFirst = new BestFirstStoreManager(getModelStore(), maxNumberOfSolutions);
		bestFirst.startExploration(initialVersion, randomSeed);
		var solutions = bestFirst.getSolutionStore().getSolutions();
		if (solutions.isEmpty()) {
			return GeneratorResult.UNSATISFIABLE;
		}
		getModel().restore(solutions.getFirst().version());
		solutionStore = bestFirst.getSolutionStore();
		return GeneratorResult.SUCCESS;
	}

	public void generate() {
		tryGenerate().orThrow();
	}

	public GeneratorResult tryGenerateWithTimeout(long l, TimeUnit timeUnit) {
		try (var executorService = Executors.newSingleThreadScheduledExecutor()) {
			var timeoutFuture = executorService.schedule(cancellationToken::cancel, l, timeUnit);
			try {
				return tryGenerate();
			} catch (GeneratorTimeoutException e) {
				return GeneratorResult.TIMEOUT;
			} finally {
				timeoutFuture.cancel(true);
				cancellationToken.reset();
			}
		}
	}

	public void generateWithTimeout(long l, TimeUnit timeUnit) {
		tryGenerateWithTimeout(l, timeUnit).orThrow();
	}

	@Override
	public <A extends AbstractValue<A, C>, C> PartialInterpretation<A, C> getPartialInterpretation(
			PartialSymbol<A, C> partialSymbol) {
		checkSuccessfulGeneration();
		var partialInterpretation = super.getPartialInterpretation(partialSymbol);
		return keepNonExistingObjects ? partialInterpretation :
				new FilteredInterpretation<>(partialInterpretation, existsInterpretation);
	}

	@Override
	public Problem serialize() {
		checkSuccessfulGeneration();
		var serializer = solutionSerializerProvider.get();
		return serializer.serializeSolution(getProblemTrace(), getModel());
	}

	private void checkSuccessfulGeneration() {
		if (!isLastGenerationSuccessful()) {
			throw new IllegalStateException("No generated model is available");
		}
	}
}
