/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import tools.refinery.store.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.Strategy;
import tools.refinery.store.dse.objectives.Fitness;

public class DepthFirstStrategy implements Strategy {

	private DesignSpaceExplorationAdapter dseAdapter;

	private int maxDepth = Integer.MAX_VALUE;
	private int maxSolutions = Integer.MAX_VALUE;
	private boolean backtrackFromSolution = true;

	public DepthFirstStrategy withDepthLimit(int maxDepth) {
		if (maxDepth >= 0) {
			this.maxDepth = maxDepth;
		}
		return this;
	}

	public DepthFirstStrategy withSolutionLimit(int maxSolutions) {
		if (maxSolutions >= 0) {
			this.maxSolutions = maxSolutions;
		}
		return this;
	}

	public DepthFirstStrategy continueIfHardObjectivesFulfilled() {
		backtrackFromSolution = false;
		return this;
	}

	@Override
	public void initialize(DesignSpaceExplorationAdapter designSpaceExplorationAdapter) {
		this.dseAdapter = designSpaceExplorationAdapter;
	}

	@Override
	public void explore() {
		if (maxSolutions == 0) {
			return;
		}
		while (dseAdapter.getSolutions().size() < maxSolutions) {
			if (!checkAndHandleGlobalConstraints()) {
				return;
			}

			Fitness fitness = dseAdapter.getFitness();
			if (fitness.isSatisfiesHardObjectives()) {
				dseAdapter.newSolution();
				if (backtrackFromSolution && !dseAdapter.backtrack()) {
					return;
				}
			}

			if (!checkAndHandleDepth()) {
				return;
			}

			if (!backtrackToLastUntraversed()) {
				return;
			}

			dseAdapter.fireRandomActivation();
		}
	}

	private boolean checkAndHandleGlobalConstraints() {
		return dseAdapter.checkGlobalConstraints() || dseAdapter.backtrack();
	}

	private boolean checkAndHandleDepth() {
		return dseAdapter.getDepth() < maxDepth || dseAdapter.backtrack();
	}

	private boolean backtrackToLastUntraversed() {
		while (dseAdapter.getUntraversedActivations().isEmpty()) {
			if (!dseAdapter.backtrack()) {
				return false;
			}
		}
		return true;
	}
}
