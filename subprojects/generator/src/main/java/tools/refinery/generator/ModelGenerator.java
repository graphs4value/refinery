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
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.statespace.SolutionStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;

public class ModelGenerator extends ModelFacade {
	private final Version initialVersion;
	private final Provider<SolutionSerializer> solutionSerializerProvider;
	private long randomSeed = 1;
	private int maxNumberOfSolutions = 1;
	private SolutionStore solutionStore;

	ModelGenerator(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed,
				   Provider<SolutionSerializer> solutionSerializerProvider) {
		super(problemTrace, store, modelSeed, Concreteness.CANDIDATE);
		this.solutionSerializerProvider = solutionSerializerProvider;
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

	// This method only makes sense if it returns {@code true} on success.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean tryGenerate() {
		solutionStore = null;
		randomSeed++;
		var bestFirst = new BestFirstStoreManager(getModelStore(), maxNumberOfSolutions);
		bestFirst.startExploration(initialVersion, randomSeed);
		var solutions = bestFirst.getSolutionStore().getSolutions();
		if (solutions.isEmpty()) {
			return false;
		}
		getModel().restore(solutions.getFirst().version());
		solutionStore = bestFirst.getSolutionStore();
		return true;
	}

	public void generate() {
		if (!tryGenerate()) {
			throw new UnsatisfiableProblemException();
		}
	}

	@Override
	public <A extends AbstractValue<A, C>, C> PartialInterpretation<A, C> getPartialInterpretation(
			PartialSymbol<A, C> partialSymbol) {
		checkSuccessfulGeneration();
		return super.getPartialInterpretation(partialSymbol);
	}

	public Problem serializeSolution() {
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
