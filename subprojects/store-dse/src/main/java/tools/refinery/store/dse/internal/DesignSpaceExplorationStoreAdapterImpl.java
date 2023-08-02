package tools.refinery.store.dse.internal;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.dse.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.Strategy;
import tools.refinery.store.dse.objectives.Objective;

import java.util.LinkedHashSet;
import java.util.List;

public class DesignSpaceExplorationStoreAdapterImpl implements DesignSpaceExplorationStoreAdapter {
	private final ModelStore store;
	private final LinkedHashSet<TransformationRule> transformationSpecifications;
	private final LinkedHashSet<RelationalQuery> globalConstraints;
	private final List<Objective> objectives;
	private final Strategy strategy;

	public DesignSpaceExplorationStoreAdapterImpl(ModelStore store,
												  LinkedHashSet<TransformationRule> transformationSpecifications,
												  LinkedHashSet<RelationalQuery> globalConstraints,
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
	public ModelAdapter createModelAdapter(Model model) {
		return new DesignSpaceExplorationAdapterImpl(model, this);
	}

	public LinkedHashSet<TransformationRule> getTransformationSpecifications() {
		return transformationSpecifications;
	}

	public LinkedHashSet<RelationalQuery> getGlobalConstraints() {
		return globalConstraints;
	}

	public List<Objective> getObjectives() {
		return objectives;
	}

	public Strategy getStrategy() {
		return strategy;
	}
}
