/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.internal.ActivationStoreWorker;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.visualization.statespace.VisualizationStore;

import java.util.Random;

public class BestFirstWorker {
	final BestFirstStoreManager storeManager;
	final Model model;
	final ActivationStoreWorker activationStoreWorker;
	final StateCoderAdapter stateCoderAdapter;
	final DesignSpaceExplorationAdapter explorationAdapter;
	final ModelQueryAdapter queryAdapter;
	final @Nullable PropagationAdapter propagationAdapter;
	final VisualizationStore visualizationStore;
	final boolean isVisualizationEnabled;

	public BestFirstWorker(BestFirstStoreManager storeManager, Model model) {
		this.storeManager = storeManager;
		this.model = model;

		explorationAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);
		queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		propagationAdapter = model.tryGetAdapter(PropagationAdapter.class).orElse(null);
		activationStoreWorker = new ActivationStoreWorker(storeManager.getActivationStore(),
				explorationAdapter.getTransformations());
		visualizationStore = storeManager.getVisualizationStore();
		isVisualizationEnabled = visualizationStore != null;
	}

	protected VersionWithObjectiveValue last = null;

	public SubmitResult submit() {
		checkSynchronized();
		if (queryAdapter.hasPendingChanges()) {
			throw new AssertionError("Pending changes detected before model submission");
		}
		if (explorationAdapter.checkExclude()) {
			return new SubmitResult(false, false, null, null);
		}

		var code = stateCoderAdapter.calculateStateCode();

		boolean isNew = storeManager.getEquivalenceClassStore().submit(code);
		if (isNew) {
			Version version = model.commit();
			ObjectiveValue objectiveValue = explorationAdapter.getObjectiveValue();
			var versionWithObjectiveValue = new VersionWithObjectiveValue(version, objectiveValue);
			last = versionWithObjectiveValue;
			var accepted = explorationAdapter.checkAccept();

			storeManager.getObjectiveStore().submit(versionWithObjectiveValue);
			storeManager.getActivationStore().markNewAsVisited(versionWithObjectiveValue, activationStoreWorker.calculateEmptyActivationSize());
			if(accepted) {
				storeManager.solutionStore.submit(versionWithObjectiveValue);
			}

			if (isVisualizationEnabled) {
				visualizationStore.addState(last.version(), last.objectiveValue().toString());
				if (accepted) {
					visualizationStore.addSolution(last.version());
				}
			}

			return new SubmitResult(true, accepted, objectiveValue, last);
		}

		return new SubmitResult(false, false, null, null);
	}

	public void restoreToLast() {
		if (explorationAdapter.getModel().hasUncommittedChanges()) {
			explorationAdapter.getModel().restore(last.version());
		}
	}

	public VersionWithObjectiveValue restoreToBest() {
		var bestVersion = storeManager.getObjectiveStore().getBest();
		last = bestVersion;
		if (bestVersion != null) {
			this.model.restore(bestVersion.version());
		}
		return last;
	}

	public VersionWithObjectiveValue restoreToRandom(Random random) {
		var objectiveStore = storeManager.getObjectiveStore();
		if (objectiveStore.getSize() == 0) {
			return null;
		}
		var randomVersion = objectiveStore.getRandom(random);
		last = randomVersion;
		if (randomVersion != null) {
			this.model.restore(randomVersion.version());
		}
		return last;
	}

	public int compare(VersionWithObjectiveValue s1, VersionWithObjectiveValue s2) {
		return storeManager.getObjectiveStore().getComparator().compare(s1, s2);
	}

	public record RandomVisitResult(SubmitResult submitResult, boolean shouldRetry) {
	}

	public RandomVisitResult visitRandomUnvisited(Random random) {
		checkSynchronized();
		if (model.hasUncommittedChanges()) {
			throw new IllegalStateException("The model has uncommitted changes!");
		}

		var visitResult = activationStoreWorker.fireRandomActivation(this.last, random);

		if (!visitResult.successfulVisit()) {
			return new RandomVisitResult(null, visitResult.mayHaveMore());
		}

		if (propagationAdapter != null) {
			var propagationResult = propagationAdapter.propagate();
			if (propagationResult.isRejected()) {
				return new RandomVisitResult(null, visitResult.mayHaveMore());
			}
		}
		queryAdapter.flushChanges();

		Version oldVersion = null;
		if (isVisualizationEnabled) {
			oldVersion = last.version();
		}
		var submitResult = submit();
		if (isVisualizationEnabled && submitResult.newVersion() != null) {
			var newVersion = submitResult.newVersion().version();
			visualizationStore.addTransition(oldVersion, newVersion,
					"fire: " + visitResult.transformation() + ", " + visitResult.activation());
		}
		return new RandomVisitResult(submitResult, visitResult.mayHaveMore());
	}

	public boolean hasEnoughSolution() {
		return storeManager.solutionStore.hasEnoughSolution();
	}

	private void checkSynchronized() {
		if (last != null && !last.version().equals(model.getState())) {
			throw new AssertionError("Worker is not synchronized with model state");
		}
	}
}
