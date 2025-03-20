/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.internal;

import tools.refinery.store.dse.transition.DecisionRule;
import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.CriterionCalculator;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.dse.transition.objectives.ObjectiveCalculator;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.resultset.PriorityAgenda;

import java.util.List;

public class DesignSpaceExplorationStoreAdapterImpl implements DesignSpaceExplorationStoreAdapter {
	protected final ModelStore store;

	protected final List<DecisionRule> decisionRules;
	protected final List<Criterion> accepts;
	protected final List<Criterion> excludes;
	protected final List<Objective> objectives;

	public DesignSpaceExplorationStoreAdapterImpl(
			ModelStore store, List<DecisionRule> decisionRules, List<Criterion> accepts, List<Criterion> excludes,
			List<Objective> objectives) {
		this.store = store;
		this.decisionRules = decisionRules;
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
		var agenda = new PriorityAgenda();
		final List<Transformation> t = this.decisionRules.stream()
				.map(x -> new Transformation(model, agenda, x))
				.toList();
		final List<CriterionCalculator> a = this.accepts.stream().map(x -> x.createCalculator(model)).toList();
		final List<CriterionCalculator> e = this.excludes.stream().map(x -> x.createCalculator(model)).toList();
		final List<ObjectiveCalculator> o = this.objectives.stream().map(x -> x.createCalculator(model)).toList();

		return new DesignSpaceExplorationAdapterImpl(model, this, t, a, e, o);
	}

	@Override
	public List<DecisionRule> getTransformations() {
		return decisionRules;
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
