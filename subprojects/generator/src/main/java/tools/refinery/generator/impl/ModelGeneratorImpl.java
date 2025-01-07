/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import tools.refinery.generator.GeneratorResult;
import tools.refinery.generator.GeneratorTimeoutException;
import tools.refinery.generator.ModelGenerator;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.dse.propagation.PropagationRejectedException;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.statespace.SolutionStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.representation.PartialSymbol;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ModelGeneratorImpl extends ConcreteModelFacade implements ModelGenerator {
	private final Version initialVersion;
	private final CancellableCancellationToken cancellationToken;
	private long randomSeed = 1;
	private int maxNumberOfSolutions = 1;
	private SolutionStore solutionStore;

	public ModelGeneratorImpl(Args args, CancellableCancellationToken cancellationToken) {
		super(args);
		this.cancellationToken = cancellationToken;
		initialVersion = getModel().commit();
	}

	@Override
	public long getRandomSeed() {
		return randomSeed;
	}

	@Override
	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
		this.solutionStore = null;
	}

	@Override
	public int getMaxNumberOfSolutions() {
		return maxNumberOfSolutions;
	}

	@Override
	public void setMaxNumberOfSolutions(int maxNumberOfSolutions) {
		this.maxNumberOfSolutions = maxNumberOfSolutions;
		this.solutionStore = null;
	}

	@Override
	public int getSolutionCount() {
		if (!isLastGenerationSuccessful()) {
			return 0;
		}
		return this.solutionStore.getSolutions().size();
	}

	@Override
	public void loadSolution(int index) {
		if (index >= getSolutionCount()) {
			throw new IndexOutOfBoundsException("No such solution");
		}
		getModel().restore(solutionStore.getSolutions().get(index).version());
	}

	@Override
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
		try {
			bestFirst.startExploration(initialVersion, randomSeed);
		} catch (PropagationRejectedException e) {
			// Fatal propagation error.
			throw getDiagnostics().wrapPropagationRejectedException(e, getProblemTrace());
		}
		var solutions = bestFirst.getSolutionStore().getSolutions();
		if (solutions.isEmpty()) {
			return GeneratorResult.UNSATISFIABLE;
		}
		getModel().restore(solutions.getFirst().version());
		solutionStore = bestFirst.getSolutionStore();
		return GeneratorResult.SUCCESS;
	}

	@Override
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

	@Override
	public <A extends AbstractValue<A, C>, C> PartialInterpretation<A, C> getPartialInterpretation(
			PartialSymbol<A, C> partialSymbol) {
		checkSuccessfulGeneration();
		return super.getPartialInterpretation(partialSymbol);
	}

	@Override
	public Problem serialize() {
		checkSuccessfulGeneration();
		return super.serialize();
	}

	private void checkSuccessfulGeneration() {
		if (!isLastGenerationSuccessful()) {
			throw new IllegalStateException("No generated model is available");
		}
	}
}
