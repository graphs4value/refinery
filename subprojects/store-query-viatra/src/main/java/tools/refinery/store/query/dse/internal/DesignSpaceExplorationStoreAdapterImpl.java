package tools.refinery.store.query.dse.internal;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.dse.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dse.Strategy;
import tools.refinery.store.query.dse.objectives.Objective;

import java.util.List;
import java.util.Set;

public class DesignSpaceExplorationStoreAdapterImpl implements DesignSpaceExplorationStoreAdapter {
	private final ModelStore store;
	private final Set<AnyQuery> stopConditionSpecifications;
	private final Set<TransformationRule> transformationSpecifications;
	private final Set<RelationalQuery> globalConstraints;
	private final List<Objective> objectives;
	private final Strategy strategy;

	public DesignSpaceExplorationStoreAdapterImpl(ModelStore store, Set<AnyQuery> stopConditionSpecifications,
												  Set<TransformationRule> transformationSpecifications,
												  Set<RelationalQuery> globalConstraints, List<Objective> objectives,
				Strategy strategy) {
			this.store = store;
			this.stopConditionSpecifications = stopConditionSpecifications;
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
	public ModelAdapter createModelAdapter(Model model) {
		return new DesignSpaceExplorationAdapterImpl(model, this);
	}

	public Set<AnyQuery> getStopConditionSpecifications() {
		return stopConditionSpecifications;
	}

	public Set<TransformationRule> getTransformationSpecifications() {
		return transformationSpecifications;
	}

	public Set<RelationalQuery> getGlobalConstraints() {
		return globalConstraints;
	}

	public List<Objective> getObjectives() {
		return objectives;
	}

	public Strategy getStrategy() {
		return strategy;
	}
}
