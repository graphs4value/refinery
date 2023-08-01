package tools.refinery.store.query.dse.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.dse.DesignSpaceExplorationBuilder;
import tools.refinery.store.query.dse.Strategy;
import tools.refinery.store.query.dse.objectives.Objective;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

public class DesignSpaceExplorationBuilderImpl
		extends AbstractModelAdapterBuilder<DesignSpaceExplorationStoreAdapterImpl>
		implements DesignSpaceExplorationBuilder {
	private final LinkedHashSet<TransformationRule> transformationSpecifications = new LinkedHashSet<>();
	private final LinkedHashSet<RelationalQuery> globalConstraints = new LinkedHashSet<>();
	private final List<Objective> objectives = new LinkedList<>();
	private Strategy strategy;

	@Override
	protected DesignSpaceExplorationStoreAdapterImpl doBuild(ModelStore store) {
		return new DesignSpaceExplorationStoreAdapterImpl(store, transformationSpecifications, globalConstraints,
				objectives, strategy);
	}

	@Override
	public DesignSpaceExplorationBuilder transformation(TransformationRule transformationRule) {
		checkNotConfigured();
		transformationSpecifications.add(transformationRule);
		return this;
	}

	@Override
	public DesignSpaceExplorationBuilder globalConstraint(RelationalQuery globalConstraint) {
		checkNotConfigured();
		globalConstraints.add(globalConstraint);
		return this;
	}

	@Override
	public DesignSpaceExplorationBuilder objective(Objective objective) {
		checkNotConfigured();
		objectives.add(objective);
		return this;
	}

	@Override
	public DesignSpaceExplorationBuilder strategy(Strategy strategy) {
		checkNotConfigured();
		this.strategy = strategy;
		return this;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		storeBuilder.symbols(DesignSpaceExplorationAdapterImpl.NODE_COUNT_SYMBOL);
		super.doConfigure(storeBuilder);
	}
}
