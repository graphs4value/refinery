/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;

public class ModelGenerator extends ModelFacade {
	private final Version initialVersion;

	private int randomSeed = 0;

	private boolean lastGenerationSuccessful;

	public ModelGenerator(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed) {
		super(problemTrace, store, modelSeed, Concreteness.CANDIDATE);
		initialVersion = getModel().commit();
	}

	public int getRandomSeed() {
		return randomSeed;
	}

	public void setRandomSeed(int randomSeed) {
		this.randomSeed = randomSeed;
		this.lastGenerationSuccessful = false;
	}

	public boolean isLastGenerationSuccessful() {
		return lastGenerationSuccessful;
	}

	// This method only makes sense if it returns {@code true} on success.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean tryGenerate() {
		lastGenerationSuccessful = false;
		randomSeed++;
		var bestFirst = new BestFirstStoreManager(getModelStore(), 1);
		bestFirst.startExploration(initialVersion, randomSeed);
		var solutions = bestFirst.getSolutionStore().getSolutions();
		if (solutions.isEmpty()) {
			return false;
		}
		getModel().restore(solutions.get(0).version());
		lastGenerationSuccessful = true;
		return true;
	}

	public void generate() {
		if (!tryGenerate()) {
			throw new UnsatisfiableProblemException();
		}
	}

	@Override
	public <A, C> PartialInterpretation<A, C> getPartialInterpretation(PartialSymbol<A, C> partialSymbol) {
		if (!lastGenerationSuccessful) {
			throw new IllegalStateException("No generated model is available");
		}
		return super.getPartialInterpretation(partialSymbol);
	}
}
