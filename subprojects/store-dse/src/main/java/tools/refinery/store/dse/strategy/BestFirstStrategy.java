package tools.refinery.store.dse.strategy;

import tools.refinery.store.map.Version;
import tools.refinery.store.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.Strategy;
import tools.refinery.store.dse.internal.Activation;
import tools.refinery.store.dse.objectives.Fitness;
import tools.refinery.store.dse.objectives.ObjectiveComparatorHelper;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public class BestFirstStrategy implements Strategy {

	private DesignSpaceExplorationAdapter dseAdapter;

	private int maxDepth;
	private boolean backTrackIfSolution = true;
	private boolean onlyBetterFirst = false;

	private PriorityQueue<TrajectoryWithFitness> trajectoriesToExplore;

	private static class TrajectoryWithFitness {

		public List<Version> trajectory;
		public Fitness fitness;

		public TrajectoryWithFitness(List<Version> trajectory, Fitness fitness) {
			super();
			this.trajectory = trajectory;
			this.fitness = fitness;
		}

		@Override
		public String toString() {
			return trajectory.toString() + fitness.toString();
		}

	}

	public BestFirstStrategy() {
		this(-1);
	}

	public BestFirstStrategy(int maxDepth) {
		if (maxDepth < 0) {
			this.maxDepth = Integer.MAX_VALUE;
		} else {
			this.maxDepth = maxDepth;
		}
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
	public void initStrategy(DesignSpaceExplorationAdapter designSpaceExplorationAdapter) {
		this.dseAdapter = designSpaceExplorationAdapter;
		final ObjectiveComparatorHelper objectiveComparatorHelper = dseAdapter.getObjectiveComparatorHelper();

		trajectoriesToExplore = new PriorityQueue<TrajectoryWithFitness>(11,
				(o1, o2) -> objectiveComparatorHelper.compare(o2.fitness, o1.fitness));
	}

	@Override
	public void explore() {
		final ObjectiveComparatorHelper objectiveComparatorHelper = dseAdapter.getObjectiveComparatorHelper();

		boolean globalConstraintsAreSatisfied = dseAdapter.checkGlobalConstraints();
		if (!globalConstraintsAreSatisfied) {
			// "Global constraint is not satisfied in the first state. Terminate.");
			return;
		}

		final Fitness firstFitness = dseAdapter.calculateFitness();
		if (firstFitness.isSatisfiesHardObjectives()) {
			dseAdapter.newSolution();
			// "First state is a solution. Terminate.");
			if (backTrackIfSolution) {
				return;
			}
		}

		if (maxDepth == 0) {
			return;
		}

		final List<Version> firstTrajectory = dseAdapter.getTrajectory();
		TrajectoryWithFitness currentTrajectoryWithFitness = new TrajectoryWithFitness(firstTrajectory, firstFitness);
		trajectoriesToExplore.add(currentTrajectoryWithFitness);

		mainLoop: while (true) {

			if (currentTrajectoryWithFitness == null) {
				if (trajectoriesToExplore.isEmpty()) {
					// "State space is fully traversed.");
					return;
				} else {
					currentTrajectoryWithFitness = trajectoriesToExplore.element();
//					if (logger.isDebugEnabled()) {
//						 "New trajectory is chosen: " + currentTrajectoryWithFitness);
//					}
					dseAdapter.restoreTrajectory(currentTrajectoryWithFitness.trajectory);
				}
			}

			Collection<Activation> activations = dseAdapter.getUntraversedActivations();
			Iterator<Activation> iterator = activations.iterator();



			while (iterator.hasNext()) {
				final Activation nextActivation = iterator.next();
				if (!iterator.hasNext()) {
					// "Last untraversed activation of the state.");
					trajectoriesToExplore.remove(currentTrajectoryWithFitness);
				}

//				if (logger.isDebugEnabled()) {
//					 "Executing new activation: " + nextActivation);
//				}
				dseAdapter.fireActivation(nextActivation);
				if (dseAdapter.isCurrentStateAlreadyTraversed()) {
					// "The new state is already visited.");
					dseAdapter.backtrack();
				} else if (!dseAdapter.checkGlobalConstraints()) {
					// "Global constraint is not satisfied.");
					dseAdapter.backtrack();
				} else {
					final Fitness nextFitness = dseAdapter.calculateFitness();
					if (nextFitness.isSatisfiesHardObjectives()) {
						dseAdapter.newSolution();
						// "Found a solution.");
						if (backTrackIfSolution) {
							dseAdapter.backtrack();
							continue;
						}
					}
					if (dseAdapter.getDepth() >= maxDepth) {
						// "Reached max depth.");
						dseAdapter.backtrack();
						continue;
					}

					TrajectoryWithFitness nextTrajectoryWithFitness = new TrajectoryWithFitness(
							dseAdapter.getTrajectory(), nextFitness);
					trajectoriesToExplore.add(nextTrajectoryWithFitness);

					int compare = objectiveComparatorHelper.compare(currentTrajectoryWithFitness.fitness,
							nextTrajectoryWithFitness.fitness);
					if (compare < 0) {
						// "Better fitness, moving on: " + nextFitness);
						currentTrajectoryWithFitness = nextTrajectoryWithFitness;
						continue mainLoop;
					} else if (compare == 0) {
						if (onlyBetterFirst) {
							// "Equally good fitness, backtrack: " + nextFitness);
							dseAdapter.backtrack();
							continue;
						} else {
							// "Equally good fitness, moving on: " + nextFitness);
							currentTrajectoryWithFitness = nextTrajectoryWithFitness;
							continue mainLoop;
						}
					} else {
						// "Worse fitness.");
						currentTrajectoryWithFitness = null;
						continue mainLoop;
					}
				}
			}

			// "State is fully traversed.");
			trajectoriesToExplore.remove(currentTrajectoryWithFitness);
			currentTrajectoryWithFitness = null;

		}
		// "Interrupted.");

	}
}
