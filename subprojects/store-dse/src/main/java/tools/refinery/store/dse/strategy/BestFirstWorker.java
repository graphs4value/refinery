/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.internal.ActivationStoreWorker;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.visualization.ModelVisualizerAdapter;

import java.util.Random;

public class BestFirstWorker {
	final BestFirstStoreManager storeManager;
	final Model model;
	final ActivationStoreWorker activationStoreWorker;
	final StateCoderAdapter stateCoderAdapter;
	final DesignSpaceExplorationAdapter explorationAdapter;
	final ViatraModelQueryAdapter queryAdapter;
	final ModelVisualizerAdapter visualizerAdapter;
	final boolean isVisualizationEnabled;

	public BestFirstWorker(BestFirstStoreManager storeManager, Model model) {
		this.storeManager = storeManager;
		this.model = model;

		explorationAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);
		activationStoreWorker = new ActivationStoreWorker(storeManager.getActivationStore(),
				explorationAdapter.getTransformations());
		visualizerAdapter = model.getAdapter(ModelVisualizerAdapter.class);
		queryAdapter = model.getAdapter(ViatraModelQueryAdapter.class);
		System.out.println("visualizerAdapter = " + visualizerAdapter);
		isVisualizationEnabled = visualizerAdapter != null;
	}

	private VersionWithObjectiveValue last = null;

	//public boolean isIncluded

	public SubmitResult submit() {
		if (explorationAdapter.checkExclude()) {
			last = null;
			return new SubmitResult(false, false, null, null);
		}

		Version version = model.commit();
		queryAdapter.flushChanges();
		ObjectiveValue objectiveValue = explorationAdapter.getObjectiveValue();
		last = new VersionWithObjectiveValue(version, objectiveValue);
		var code = stateCoderAdapter.calculateStateCode();
		var accepted = explorationAdapter.checkAccept();
		boolean isNew = storeManager.getEquivalenceClassStore().submit(last, code,
				activationStoreWorker.calculateEmptyActivationSize(), accepted);
		return new SubmitResult(isNew, accepted, objectiveValue, isNew ? last : null);
	}

	public void restoreToLast() {
		if (explorationAdapter.getModel().hasUncommittedChanges()) {
			var oldVersion = model.getState();
			explorationAdapter.getModel().restore(last.version());
			if (isVisualizationEnabled) {
				visualizerAdapter.addTransition(oldVersion, last.version(), "");
			}
		}
	}

	public VersionWithObjectiveValue restoreToBest() {
		var bestVersion = storeManager.getObjectiveStore().getBest();
		if (bestVersion != null) {
			var oldVersion = model.getState();
			this.model.restore(bestVersion.version());
			if (isVisualizationEnabled) {
				visualizerAdapter.addTransition(oldVersion, last.version(), "");
			}
		}
		return bestVersion;
	}

	public VersionWithObjectiveValue restoreToRandom(Random random) {
		var randomVersion = storeManager.getObjectiveStore().getRandom(random);
		last = randomVersion;
		if (randomVersion != null) {
			this.model.restore(randomVersion.version());
		}
		return randomVersion;
	}

	public int compare(VersionWithObjectiveValue s1, VersionWithObjectiveValue s2) {
		return storeManager.getObjectiveStore().getComparator().compare(s1, s2);
	}

	public boolean stateHasUnvisited() {
		if (!model.hasUncommittedChanges()) {
			return storeManager.getActivationStore().hasUnmarkedActivation(last);
		} else {
			throw new IllegalStateException("The model has uncommitted changes!");
		}
	}

	record RandomVisitResult(SubmitResult submitResult, boolean shouldRetry) {
	}

	public RandomVisitResult visitRandomUnvisited(Random random) {
		if (!model.hasUncommittedChanges()) {
			var visitResult = activationStoreWorker.fireRandomActivation(this.last, random);
			if (visitResult.successfulVisit()) {
				Version oldVersion = null;
				if (isVisualizationEnabled) {
					oldVersion = last.version();
				}
				var submitResult = submit();
				if (isVisualizationEnabled) {

					Version newVersion = null;
					if (submitResult.newVersion() != null) {
						newVersion = submitResult.newVersion().version();
						visualizerAdapter.addState(newVersion, submitResult.newVersion().objectiveValue().toString());
						visualizerAdapter.addSolution(newVersion);
					}
					visualizerAdapter.addTransition(oldVersion, newVersion, "");
				}
				return new RandomVisitResult(submitResult, visitResult.mayHaveMore());
			} else {
				return new RandomVisitResult(null, visitResult.mayHaveMore());
			}
		} else {
			throw new IllegalStateException("The model has uncommitted changes!");
		}
	}

	public boolean hasEnoughSolution() {
		return storeManager.solutionStore.hasEnoughSolution();
	}
}
