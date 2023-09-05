/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import org.eclipse.collections.api.block.procedure.Procedure;
import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.dse.transition.statespace.EquivalenceClassStore;
import tools.refinery.store.dse.transition.statespace.ObjectivePriorityQueue;
import tools.refinery.store.dse.transition.statespace.SolutionStore;
import tools.refinery.store.dse.transition.statespace.internal.ActivationStoreImpl;
import tools.refinery.store.dse.transition.statespace.internal.FastEquivalenceClassStore;
import tools.refinery.store.dse.transition.statespace.internal.ObjectivePriorityQueueImpl;
import tools.refinery.store.dse.transition.statespace.internal.SolutionStoreImpl;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;

public class BestFirstStoreManager {
	ModelStore modelStore;
	ObjectivePriorityQueue objectiveStore;
	ActivationStore activationStore;
	SolutionStore solutionStore;
	EquivalenceClassStore equivalenceClassStore;

	public BestFirstStoreManager(ModelStore modelStore) {
		this.modelStore = modelStore;
		DesignSpaceExplorationStoreAdapter storeAdapter =
				modelStore.getAdapter(DesignSpaceExplorationStoreAdapter.class);

		objectiveStore = new ObjectivePriorityQueueImpl(storeAdapter.getObjectives());
		Procedure<VersionWithObjectiveValue> whenAllActivationsVisited = x -> objectiveStore.remove(x);
		activationStore = new ActivationStoreImpl(storeAdapter.getTransformations().size(), whenAllActivationsVisited);
		solutionStore = new SolutionStoreImpl(1);
		equivalenceClassStore = new FastEquivalenceClassStore(modelStore.getAdapter(StateCoderStoreAdapter.class)) {
			@Override
			protected void delegate(VersionWithObjectiveValue version, int[] emptyActivations, boolean accept) {
				objectiveStore.submit(version);
				activationStore.markNewAsVisited(version, emptyActivations);
				if(accept) {
					solutionStore.submit(version);
				}
			}
		};
	}

	ObjectivePriorityQueue getObjectiveStore() {
		return objectiveStore;
	}

	ActivationStore getActivationStore() {
		return activationStore;
	}

	SolutionStore getSolutionStore() {
		return solutionStore;
	}

	EquivalenceClassStore getEquivalenceClassStore() {
		return equivalenceClassStore;
	}

	public void startExploration(Version initial) {
		BestFirstExplorer bestFirstExplorer = new BestFirstExplorer(this, modelStore.createModelForState(initial), 1);
		bestFirstExplorer.explore();
	}
}
