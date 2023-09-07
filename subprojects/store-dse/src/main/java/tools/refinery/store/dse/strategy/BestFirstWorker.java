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
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.statecoding.StateCoderAdapter;

import java.util.Random;

public class BestFirstWorker {
	final BestFirstStoreManager storeManager;
	final Model model;
	final ActivationStoreWorker activationStoreWorker;
	final StateCoderAdapter stateCoderAdapter;
	final DesignSpaceExplorationAdapter explorationAdapter;
	final ModelQueryAdapter queryAdapter;

	public BestFirstWorker(BestFirstStoreManager storeManager, Model model) {
		this.storeManager = storeManager;
		this.model = model;

		explorationAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);
		queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		activationStoreWorker = new ActivationStoreWorker(storeManager.getActivationStore(),
				explorationAdapter.getTransformations());
	}

	private VersionWithObjectiveValue last = null;

	//public boolean isIncluded

	public SubmitResult submit() {
		if (explorationAdapter.checkExclude()) {
			last = null;
			return new SubmitResult(false, false, null, null);
		}

		Version version = model.commit();
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
			explorationAdapter.getModel().restore(last.version());
		}
	}

	public VersionWithObjectiveValue restoreToBest() {
		var bestVersion = storeManager.getObjectiveStore().getBest();
		if (bestVersion != null) {
			this.model.restore(bestVersion.version());
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
			queryAdapter.flushChanges();
			var visitResult = activationStoreWorker.fireRandomActivation(this.last, random);

			if (visitResult.successfulVisit()) {
				return new RandomVisitResult(submit(), visitResult.mayHaveMore());
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
