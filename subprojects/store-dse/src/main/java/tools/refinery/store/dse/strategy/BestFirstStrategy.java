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

import tools.refinery.store.map.Version;
import tools.refinery.store.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.Strategy;
import tools.refinery.store.dse.internal.Activation;
import tools.refinery.store.dse.objectives.Fitness;
import tools.refinery.store.dse.objectives.ObjectiveComparatorHelper;

import java.util.*;

public class BestFirstStrategy implements Strategy {

	private DesignSpaceExplorationAdapter dseAdapter;

	private int maxDepth = Integer.MAX_VALUE;
	private int maxSolutions = Integer.MAX_VALUE;
	private boolean backTrackIfSolution = true;
	private boolean onlyBetterFirst = false;

	private PriorityQueue<TrajectoryWithFitness> trajectoriesToExplore;

	private record TrajectoryWithFitness(List<Version> trajectory, Fitness fitness) {
		@Override
		public String toString() {
				return trajectory.toString() + fitness.toString();
			}

		@Override
		public int hashCode() {
			return trajectory.get(trajectory.size() - 1).hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TrajectoryWithFitness other) {
				return Objects.equals(trajectory.get(trajectory.size() - 1), other.trajectory.get(other.trajectory.size() - 1));
//				return trajectory.equals(((TrajectoryWithFitness) obj).trajectory);
			}
			return false;
		}
	}

	public BestFirstStrategy withDepthLimit(int maxDepth) {
		if (maxDepth >= 0) {
			this.maxDepth = maxDepth;
		}
		return this;
	}

	public BestFirstStrategy withSolutionLimit(int maxSolutions) {
		if (maxSolutions >= 0) {
			this.maxSolutions = maxSolutions;
		}
		return this;
	}

	public BestFirstStrategy continueIfHardObjectivesFulfilled() {
		backTrackIfSolution = false;
		return this;
	}

	public BestFirstStrategy goOnOnlyIfFitnessIsBetter() {
		onlyBetterFirst = true;
		return this;
	}

	@Override
	public void initialize(DesignSpaceExplorationAdapter designSpaceExplorationAdapter) {
		this.dseAdapter = designSpaceExplorationAdapter;
		final ObjectiveComparatorHelper objectiveComparatorHelper = dseAdapter.getObjectiveComparatorHelper();

		trajectoriesToExplore = new PriorityQueue<>(11,
				(o1, o2) -> objectiveComparatorHelper.compare(o2.fitness, o1.fitness));
	}

	@Override
	public void explore() {
		if (maxSolutions == 0) {
			return;
		}
		final ObjectiveComparatorHelper objectiveComparatorHelper = dseAdapter.getObjectiveComparatorHelper();

		boolean globalConstraintsAreSatisfied = dseAdapter.checkGlobalConstraints();
		if (!globalConstraintsAreSatisfied) {
			// Global constraint is not satisfied in the first state. Terminate.
			return;
		}

		final Fitness firstFitness = dseAdapter.getFitness();
		if (firstFitness.isSatisfiesHardObjectives()) {
			dseAdapter.newSolution();
			// First state is a solution. Terminate.
			if (backTrackIfSolution) {
				return;
			}
		}

		if (maxDepth == 0) {
			return;
		}


		var firstTrajectoryWithFitness = new TrajectoryWithFitness(dseAdapter.getTrajectory(), firstFitness);
		trajectoriesToExplore.add(firstTrajectoryWithFitness);
		TrajectoryWithFitness currentTrajectoryWithFitness = null;

		mainLoop: while (true) {

			if (currentTrajectoryWithFitness == null) {
				if (trajectoriesToExplore.isEmpty()) {
					// State space is fully traversed.
					return;
				} else {
					currentTrajectoryWithFitness = trajectoriesToExplore.element();
					// New trajectory is chosen: " + currentTrajectoryWithFitness
					dseAdapter.restoreTrajectory(currentTrajectoryWithFitness.trajectory);
				}
			}

			Collection<Activation> activations = dseAdapter.getUntraversedActivations();
			Iterator<Activation> iterator = activations.iterator();



			while (iterator.hasNext()) {
				final Activation nextActivation = iterator.next();
				if (!iterator.hasNext()) {
					// Last untraversed activation of the state.
					trajectoriesToExplore.remove(currentTrajectoryWithFitness);
				}

				// Executing new activation
				dseAdapter.fireActivation(nextActivation);
				if (dseAdapter.isCurrentStateAlreadyTraversed()) {
					// The new state is already visited.
					dseAdapter.backtrack();
				} else if (!dseAdapter.checkGlobalConstraints()) {
					// Global constraint is not satisfied.
					dseAdapter.backtrack();
				} else {
					final Fitness nextFitness = dseAdapter.getFitness();
					if (nextFitness.isSatisfiesHardObjectives()) {
						dseAdapter.newSolution();
						var solutions = dseAdapter.getSolutions().size();
						if (solutions >= maxSolutions) {
							return;
						}
						// Found a solution.
						if (backTrackIfSolution) {
							dseAdapter.backtrack();
							continue;
						}
					}
					if (dseAdapter.getDepth() >= maxDepth) {
						// Reached max depth.
						dseAdapter.backtrack();
						continue;
					}

					TrajectoryWithFitness nextTrajectoryWithFitness = new TrajectoryWithFitness(
							dseAdapter.getTrajectory(), nextFitness);
					trajectoriesToExplore.add(nextTrajectoryWithFitness);

					int compare = objectiveComparatorHelper.compare(currentTrajectoryWithFitness.fitness,
							nextTrajectoryWithFitness.fitness);
					if (compare < 0) {
						// Better fitness, moving on
						currentTrajectoryWithFitness = nextTrajectoryWithFitness;
						continue mainLoop;
					} else if (compare == 0) {
						if (onlyBetterFirst) {
							// Equally good fitness, backtrack
							dseAdapter.backtrack();
						} else {
							// Equally good fitness, moving on
							currentTrajectoryWithFitness = nextTrajectoryWithFitness;
							continue mainLoop;
						}
					} else {
						//"Worse fitness
						currentTrajectoryWithFitness = null;
						continue mainLoop;
					}
				}
			}

			// State is fully traversed.
			currentTrajectoryWithFitness = null;

		}
		// Interrupted.
	}
}
