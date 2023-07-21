package tools.refinery.store.query.dse.strategy;

import tools.refinery.store.query.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.query.dse.Strategy;
import tools.refinery.store.query.dse.internal.Activation;
import tools.refinery.store.query.dse.objectives.Fitness;

import java.util.Collection;

public class DepthFirstStrategy implements Strategy {

	private DesignSpaceExplorationAdapter dseAdapter;

	private int maxDepth;
	private boolean backTrackIfSolution = true;

	public DepthFirstStrategy() {
		this(-1);
	}

	public DepthFirstStrategy(int maxDepth) {
		if (maxDepth < 0) {
			this.maxDepth = Integer.MAX_VALUE;
		} else {
			this.maxDepth = maxDepth;
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
		mainloop: while (true) {
			var globalConstraintsAreSatisfied = dseAdapter.checkGlobalConstraints();
			if (!globalConstraintsAreSatisfied) {
				var isSuccessfulUndo = dseAdapter.backtrack();
				if (!isSuccessfulUndo) {
//					"Global constraint is not satisfied and cannot backtrack."
					break;
				}
				else {
//					"Global constraint is not satisfied, backtrack."
					continue;
				}
			}

			Fitness fitness = dseAdapter.calculateFitness();
			if (fitness.isSatisfiesHardObjectives()) {
				dseAdapter.newSolution();
				if (backTrackIfSolution) {
					var isSuccessfulUndo = dseAdapter.backtrack();
					if (!isSuccessfulUndo) {
//					"Found a solution but cannot backtrack."
						break;
					} else {
//					"Found a solution, backtrack."
						continue;
					}
				}
			}

			var depth = dseAdapter.getDepth();
			if (dseAdapter.getDepth() >= maxDepth) {
				var isSuccessfulUndo = dseAdapter.backtrack();
				if (!isSuccessfulUndo) {
//					"Reached max depth but cannot backtrack."
					break;
				}
			}

			Collection<Activation> activations;
			do {
				activations = dseAdapter.getUntraversedActivations();
				if (activations.isEmpty()) {
					if (!dseAdapter.backtrack()) {
	//					"No more transitions from current state and cannot backtrack."
						break mainloop;
					}
					else {
	//					"No more transitions from current state, backtrack."
						continue;
					}
				}
			} while (activations.isEmpty());

			dseAdapter.fireRandomActivation();
//			if (dseAdapter.isCurrentInTrajectory()) {
//				if (!dseAdapter.backtrack()) {
////					TODO: throw exception
////					"The new state is present in the trajectory but cannot backtrack. Should never happen!"
//					break;
//				}
//				else {
////					"The new state is already visited in the trajectory, backtrack."
//					continue;
//				}
//			}
		}
	}
}
