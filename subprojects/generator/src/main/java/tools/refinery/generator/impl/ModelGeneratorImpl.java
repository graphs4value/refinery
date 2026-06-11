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
import tools.refinery.store.dse.transition.statespace.SolutionStoreListener;
import tools.refinery.store.map.Version;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.representation.PartialSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ModelGeneratorImpl extends ConcreteModelFacade implements ModelGenerator {
	private final Version initialVersion;
	private final CancellableCancellationToken cancellationToken;
	private final List<SolutionStoreListener> listeners = new ArrayList<>();
	private long randomSeed = 1;
	private int maxNumberOfSolutions = 1;
	private Status status = Status.RESET;
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
		clearLastGeneration();
	}

	@Override
	public int getMaxNumberOfSolutions() {
		return maxNumberOfSolutions;
	}

	@Override
	public void setMaxNumberOfSolutions(int maxNumberOfSolutions) {
		this.maxNumberOfSolutions = maxNumberOfSolutions;
		clearLastGeneration();
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

	private void clearLastGeneration() {
		status = Status.RESET;
		solutionStore = null;
	}

	private void generationFailed() {
		status = Status.FAILED;
		solutionStore = null;
	}

	@Override
	public boolean isLastGenerationSuccessful() {
		return status == Status.SUCCESS;
	}

	@Override
	public boolean hasEnoughSolutions() {
		if (solutionStore == null) {
			return false;
		}
		return solutionStore.hasEnoughSolution();
	}

	public GeneratorResult tryGenerate() {
		if (status == Status.RUNNING) {
			throw new IllegalStateException("Another model generation is already running");
		}
		if (cancellationToken.isCancelled()) {
			throw new IllegalStateException("Model generation was previously cancelled");
		}
		clearLastGeneration();
		randomSeed++;
		var bestFirst = new BestFirstStoreManager(getModelStore(), maxNumberOfSolutions);
		solutionStore = bestFirst.getSolutionStore();
		listeners.forEach(solutionStore::addListener);
		status = Status.RUNNING;
		try {
			try {
				bestFirst.startExploration(initialVersion, randomSeed);
			} catch (PropagationRejectedException e) {
				// Fatal propagation error.
				throw getDiagnostics().wrapPropagationRejectedException(e, getProblemTrace());
			}
			var solutions = bestFirst.getSolutionStore().getSolutions();
			if (solutions.isEmpty()) {
				generationFailed();
				return GeneratorResult.UNSATISFIABLE;
			}
			getModel().restore(solutions.getFirst().version());
		} catch (RuntimeException e) {
			generationFailed();
			throw e;
		}
		status = Status.SUCCESS;
		return hasEnoughSolutions() ? GeneratorResult.REQUEST_FULFILLED : GeneratorResult.NO_MORE_SOLUTIONS;
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
		checkModelAccess();
		return super.getPartialInterpretation(partialSymbol);
	}

	@Override
	public Problem serialize() {
		checkModelAccess();
		return super.serialize();
	}

	private void checkModelAccess() {
		switch (status) {
		case RESET -> throw new IllegalStateException("No generated model is available");
		case FAILED -> throw new IllegalStateException("Last model generation has failed");
		case RUNNING, SUCCESS -> {
			// Access is allowed.
		}
		}
	}

	@Override
	public void addListener(SolutionStoreListener listener) {
		listeners.add(listener);
		if (solutionStore != null) {
			solutionStore.addListener(listener);
		}
	}

	@Override
	public void removeListener(SolutionStoreListener listener) {
		listeners.remove(listener);
		if (solutionStore != null) {
			solutionStore.removeListener(listener);
		}
	}

	private enum Status {
		RESET,
		RUNNING,
		SUCCESS,
		FAILED,
	}
}
