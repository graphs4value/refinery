/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

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
import tools.refinery.visualization.statespace.VisualizationStore;
import tools.refinery.visualization.statespace.internal.VisualizationStoreImpl;

import java.util.function.Consumer;

public class BestFirstStoreManager {

	ModelStore modelStore;
	ObjectivePriorityQueue objectiveStore;
	ActivationStore activationStore;
	SolutionStore solutionStore;
	EquivalenceClassStore equivalenceClassStore;
	VisualizationStore visualizationStore;

	public BestFirstStoreManager(ModelStore modelStore, int maxNumberOfSolutions) {
		this.modelStore = modelStore;
		DesignSpaceExplorationStoreAdapter storeAdapter =
				modelStore.getAdapter(DesignSpaceExplorationStoreAdapter.class);

		objectiveStore = new ObjectivePriorityQueueImpl(storeAdapter.getObjectives());
		Consumer<VersionWithObjectiveValue> whenAllActivationsVisited = x -> objectiveStore.remove(x);
		activationStore = new ActivationStoreImpl(storeAdapter.getTransformations().size(), whenAllActivationsVisited);
		solutionStore = new SolutionStoreImpl(maxNumberOfSolutions);
		equivalenceClassStore = new FastEquivalenceClassStore(modelStore.getAdapter(StateCoderStoreAdapter.class)) {
			@Override
			protected void delegate(VersionWithObjectiveValue version, int[] emptyActivations, boolean accept) {
				throw new UnsupportedOperationException("This equivalence storage is not prepared to resolve " +
						"symmetries!");
			}
		};
		visualizationStore = new VisualizationStoreImpl();
	}

	public ModelStore getModelStore() {
		return modelStore;
	}

	ObjectivePriorityQueue getObjectiveStore() {
		return objectiveStore;
	}

	ActivationStore getActivationStore() {
		return activationStore;
	}

	public SolutionStore getSolutionStore() {
		return solutionStore;
	}

	EquivalenceClassStore getEquivalenceClassStore() {
		return equivalenceClassStore;
	}

	public VisualizationStore getVisualizationStore() {
		return visualizationStore;
	}

	public void startExploration(Version initial) {
		startExploration(initial, 1);
	}

	public void startExploration(Version initial, long randomSeed) {
		BestFirstExplorer bestFirstExplorer = new BestFirstExplorer(this, modelStore.createModelForState(initial),
				randomSeed);
		bestFirstExplorer.explore();
	}
}
