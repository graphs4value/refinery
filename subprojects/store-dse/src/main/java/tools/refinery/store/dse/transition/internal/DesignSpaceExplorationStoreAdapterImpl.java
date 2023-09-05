/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.internal;

import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.dse.transition.TransformationRule;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.CriterionCalculator;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.dse.transition.objectives.ObjectiveCalculator;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;

import java.util.List;

public class DesignSpaceExplorationStoreAdapterImpl implements DesignSpaceExplorationStoreAdapter {
	protected final ModelStore store;

	protected final List<TransformationRule> transformationRuleDefinitions;
	protected final List<Criterion> accepts;
	protected final List<Criterion> excludes;
	protected final List<Objective> objectives;

	public DesignSpaceExplorationStoreAdapterImpl(ModelStore store,
												  List<TransformationRule> transformationRuleDefinitions,
												  List<Criterion> accepts, List<Criterion> excludes,
												  List<Objective> objectives) {
		this.store = store;

		this.transformationRuleDefinitions = transformationRuleDefinitions;
		this.accepts = accepts;
		this.excludes = excludes;
		this.objectives = objectives;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public DesignSpaceExplorationAdapterImpl createModelAdapter(Model model) {
		final List<Transformation> t = this.transformationRuleDefinitions.stream().map(x->x.prepare(model)).toList();
		final List<CriterionCalculator> a = this.accepts.stream().map(x->x.createCalculator(model)).toList();
		final List<CriterionCalculator> e = this.excludes.stream().map(x->x.createCalculator(model)).toList();
		final List<ObjectiveCalculator> o = this.objectives.stream().map(x->x.createCalculator(model)).toList();

		return new DesignSpaceExplorationAdapterImpl(model, this, t, a, e, o);
	}
	@Override
	public List<TransformationRule> getTransformations() {
		return transformationRuleDefinitions;
	}
	@Override
	public List<Criterion> getAccepts() {
		return accepts;
	}
	@Override
	public List<Criterion> getExcludes() {
		return excludes;
	}
	@Override
	public List<Objective> getObjectives() {
		return objectives;
	}
}
