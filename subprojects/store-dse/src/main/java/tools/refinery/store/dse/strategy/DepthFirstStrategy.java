/*******************************************************************************
 * Copyright (c) 2010-2016, Andras Szabolcs Nagy, Zoltan Ujhelyi and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.dse.strategy;

import tools.refinery.store.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.Strategy;
import tools.refinery.store.dse.objectives.Fitness;

public class DepthFirstStrategy implements Strategy {

	private DesignSpaceExplorationAdapter dseAdapter;

	private int maxDepth;
	private int maxSolutions;
	private boolean backTrackIfSolution = true;

	public DepthFirstStrategy() {
		this(-1);
	}

	public DepthFirstStrategy(int maxDepth) {
		this(maxDepth, -1);
	}

	public DepthFirstStrategy(int maxDepth, int maxSolutions) {
		if (maxDepth < 0) {
			this.maxDepth = Integer.MAX_VALUE;
		} else {
			this.maxDepth = maxDepth;
		}
		if (maxSolutions < 0) {
			this.maxSolutions = Integer.MAX_VALUE;
		} else {
			this.maxSolutions = maxSolutions;
		}
	}

	public DepthFirstStrategy continueIfHardObjectivesFulfilled() {
		backTrackIfSolution = false;
		return this;
	}

	@Override
	public void initStrategy(DesignSpaceExplorationAdapter designSpaceExplorationAdapter) {
		this.dseAdapter = designSpaceExplorationAdapter;
	}

	@Override
	public void explore() {
		if (maxSolutions == 0) {
			return;
		}
		while (dseAdapter.getSolutions().size() < maxSolutions) {
			if (!checkAndHandleGlobalConstraints()) {
				// Global constraint is not satisfied and cannot backtrack.
				return;
			}
			// Global constraint is not satisfied, backtrack.

			Fitness fitness = dseAdapter.getFitness();
			if (fitness.isSatisfiesHardObjectives()) {
				dseAdapter.newSolution();
				if (backTrackIfSolution && !dseAdapter.backtrack()) {
					// Found a solution but cannot backtrack.
					return;
				}
			}

			if (!checkAndHandleDepth()) {
				// Reached max depth but cannot backtrack.
				return;
			}

			if (!backtrackToLastUntraversed()) {
				return;
			}

			dseAdapter.fireRandomActivation();
		}
	}

	private boolean checkAndHandleGlobalConstraints() {
		// Global constraint is not satisfied and cannot backtrack.
		return dseAdapter.checkGlobalConstraints() || dseAdapter.backtrack();
		// Global constraint is satisfied or backtrack.
	}

	private boolean checkAndHandleDepth() {
		// Reached max depth but cannot backtrack.
		return dseAdapter.getDepth() < maxDepth || dseAdapter.backtrack();
		// Reached max depth or backtrack.
	}

	private boolean backtrackToLastUntraversed() {
		while (dseAdapter.getUntraversedActivations().isEmpty()) {
			if (!dseAdapter.backtrack()) {
				// No more transitions from current state and cannot backtrack.
				return false;
			}
			// No more transitions from current state, backtrack.
		}
		return true;
	}
}
