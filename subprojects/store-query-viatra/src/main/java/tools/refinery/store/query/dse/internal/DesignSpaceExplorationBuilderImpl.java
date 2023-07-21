package tools.refinery.store.query.dse.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.dse.DesignSpaceExplorationBuilder;
import tools.refinery.store.query.dse.Strategy;
import tools.refinery.store.query.dse.objectives.Objective;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DesignSpaceExplorationBuilderImpl
		extends AbstractModelAdapterBuilder<DesignSpaceExplorationStoreAdapterImpl>
		implements DesignSpaceExplorationBuilder {

	private final Set<AnyQuery> stopConditionSpecifications = new LinkedHashSet<>();
	private final Set<TransformationRule> transformationSpecifications = new LinkedHashSet<>();
	private final Set<RelationalQuery> globalConstraints = new LinkedHashSet<>();
	private final List<Objective> objectives = new LinkedList<>();
	private Strategy strategy;

	@Override
	protected DesignSpaceExplorationStoreAdapterImpl doBuild(ModelStore store) {
		return new DesignSpaceExplorationStoreAdapterImpl(store, stopConditionSpecifications,
				transformationSpecifications, globalConstraints, objectives, strategy);
	}

	@Override
	public DesignSpaceExplorationBuilder stopCondition(AnyQuery stopCondition) {
		checkNotConfigured();
		stopConditionSpecifications.add(stopCondition);
		return this;
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

	public Set<AnyQuery> getStopConditionSpecifications() {
		return stopConditionSpecifications;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		storeBuilder.symbols(DesignSpaceExplorationAdapterImpl.NODE_COUNT_SYMBOL);
		super.doConfigure(storeBuilder);
	}
}
