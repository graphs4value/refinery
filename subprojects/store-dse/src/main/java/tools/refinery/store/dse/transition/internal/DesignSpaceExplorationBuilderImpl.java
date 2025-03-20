/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.transition.DecisionRule;
import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryBuilder;

import java.util.LinkedHashSet;
import java.util.List;

public class DesignSpaceExplorationBuilderImpl
		extends AbstractModelAdapterBuilder<DesignSpaceExplorationStoreAdapterImpl>
		implements DesignSpaceExplorationBuilder {

	LinkedHashSet<DecisionRule> decisionRules = new LinkedHashSet<>();
	LinkedHashSet<Criterion> accepts = new LinkedHashSet<>();
	LinkedHashSet<Criterion> excludes = new LinkedHashSet<>();
	LinkedHashSet<Objective> objectives = new LinkedHashSet<>();

	@Override
	public DesignSpaceExplorationBuilder transformation(DecisionRule decisionRule) {
		decisionRules.add(decisionRule);
		return this;
	}

	@Override
	public DesignSpaceExplorationBuilder accept(Criterion criteria) {
		accepts.add(criteria);
		return this;
	}

	@Override
	public DesignSpaceExplorationBuilder exclude(Criterion criteria) {
		excludes.add(criteria);
		return this;
	}


	@Override
	public DesignSpaceExplorationBuilder objective(Objective objective) {
		objectives.add(objective);
		return this;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		var queryEngine = storeBuilder.getAdapter(ModelQueryBuilder.class);
		decisionRules.forEach(x -> queryEngine.query(x.rule().getPrecondition()));
		accepts.forEach(x -> x.configure(storeBuilder));
		excludes.forEach(x -> x.configure(storeBuilder));
		objectives.forEach(x -> x.configure(storeBuilder));

		super.doConfigure(storeBuilder);
	}

	@Override
	protected DesignSpaceExplorationStoreAdapterImpl doBuild(ModelStore store) {
		List<DecisionRule> decisionRuleList = List.copyOf(decisionRules);
		List<Criterion> acceptsList = List.copyOf(accepts);
		List<Criterion> excludesList = List.copyOf(excludes);
		List<Objective> objectivesList = List.copyOf(objectives);

		return new DesignSpaceExplorationStoreAdapterImpl(store, decisionRuleList, acceptsList,
				excludesList, objectivesList);
	}
}
