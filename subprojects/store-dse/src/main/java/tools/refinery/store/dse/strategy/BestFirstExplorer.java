/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.model.Model;

import java.util.Random;

public class BestFirstExplorer extends BestFirstWorker {
	final int id;
	Random random;
	public BestFirstExplorer(BestFirstStoreManager storeManager, Model model, int id) {
		super(storeManager, model);
		this.id = id;
		this.random = new Random(id);
	}

	private boolean interrupted = false;
	public void interrupt() {
		this.interrupted = true;
	}

	private boolean shouldRun() {
		return !interrupted && !hasEnoughSolution();
	}

	public void explore() {
		VersionWithObjectiveValue lastVisited = submit().newVersion();

		while (shouldRun()) {

			if (lastVisited == null) {
				lastVisited = this.restoreToBest();
				if(lastVisited == null) {
					return;
				}
			}

			boolean tryActivation = true;
			while(tryActivation && shouldRun()) {
				RandomVisitResult randomVisitResult = this.visitRandomUnvisited(random);

				tryActivation = randomVisitResult.shouldRetry();
				var newSubmit = randomVisitResult.submitResult();
				if(newSubmit != null) {
					if(!newSubmit.include()) {
						restoreToLast();
					} else {
						var newVisit = newSubmit.newVersion();
						int compareResult = compare(lastVisited,newVisit);
						if(compareResult >= 0) {
							lastVisited = newVisit;
							break;
						}
					}
				}
				else {
					lastVisited = null;
					break;
				}
			}

		//final ObjectiveComparatorHelper objectiveComparatorHelper = dseAdapter.getObjectiveComparatorHelper();

		/*boolean globalConstraintsAreSatisfied = dseAdapter.checkGlobalConstraints();
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
		}*/

		/*
		var firstTrajectoryWithFitness = new TrajectoryWithFitness(dseAdapter.getTrajectory(), firstFitness);
		trajectoriesToExplore.add(firstTrajectoryWithFitness);
		TrajectoryWithFitness currentTrajectoryWithFitness = null;
		*/
/*
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
*/
		}
		// Interrupted.
	}
}
