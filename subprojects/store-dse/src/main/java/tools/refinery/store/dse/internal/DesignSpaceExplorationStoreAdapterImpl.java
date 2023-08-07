/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.internal;

import tools.refinery.store.dse.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.Strategy;
import tools.refinery.store.dse.objectives.Objective;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.RelationalQuery;

import java.util.List;
import java.util.Set;

public class DesignSpaceExplorationStoreAdapterImpl implements DesignSpaceExplorationStoreAdapter {
	private final ModelStore store;
	private final Set<TransformationRule> transformationSpecifications;
	private final Set<RelationalQuery> globalConstraints;
	private final List<Objective> objectives;
	private final Strategy strategy;

	public DesignSpaceExplorationStoreAdapterImpl(ModelStore store,
												  Set<TransformationRule> transformationSpecifications,
												  Set<RelationalQuery> globalConstraints,
												  List<Objective> objectives, Strategy strategy) {
			this.store = store;
			this.transformationSpecifications = transformationSpecifications;
			this.globalConstraints = globalConstraints;
			this.objectives = objectives;
			this.strategy = strategy;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public DesignSpaceExplorationAdapterImpl createModelAdapter(Model model) {
		return new DesignSpaceExplorationAdapterImpl(model, this);
	}

	@Override
	public Set<TransformationRule> getTransformationSpecifications() {
		return transformationSpecifications;
	}

	@Override
	public Set<RelationalQuery> getGlobalConstraints() {
		return globalConstraints;
	}

	@Override
	public List<Objective> getObjectives() {
		return objectives;
	}

	@Override
	public Strategy getStrategy() {
		return strategy;
	}
}
