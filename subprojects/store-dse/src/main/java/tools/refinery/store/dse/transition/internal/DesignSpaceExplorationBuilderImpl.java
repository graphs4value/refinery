/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.TransformationRule;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class DesignSpaceExplorationBuilderImpl
		extends AbstractModelAdapterBuilder<DesignSpaceExplorationStoreAdapterImpl>
		implements DesignSpaceExplorationBuilder {

	LinkedHashSet<TransformationRule> transformationRuleDefinitions = new LinkedHashSet<>();
	LinkedHashSet<Criterion> accepts = new LinkedHashSet<>();
	LinkedHashSet<Criterion> excludes = new LinkedHashSet<>();
	LinkedHashSet<Objective> objectives = new LinkedHashSet<>();

	@Override
	public DesignSpaceExplorationBuilder transformation(TransformationRule transformationRuleDefinition) {
		transformationRuleDefinitions.add(transformationRuleDefinition);
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
		transformationRuleDefinitions.forEach(x -> x.doConfigure(storeBuilder));
		accepts.forEach(x -> x.doConfigure(storeBuilder));
		excludes.forEach(x -> x.doConfigure(storeBuilder));
		objectives.forEach(x -> x.doConfigure(storeBuilder));

		super.doConfigure(storeBuilder);
	}

	@Override
	protected DesignSpaceExplorationStoreAdapterImpl doBuild(ModelStore store) {
		List<TransformationRule> transformationRuleDefinitiions1 = new ArrayList<>(transformationRuleDefinitions);
		List<Criterion> accepts1 = new ArrayList<>(accepts);
		List<Criterion> excludes1 = new ArrayList<>(excludes);
		List<Objective> objectives1 = new ArrayList<>(objectives);

		return new DesignSpaceExplorationStoreAdapterImpl(store,
				transformationRuleDefinitiions1, accepts1,
				excludes1, objectives1);
	}
}
