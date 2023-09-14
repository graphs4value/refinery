/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation.impl.rule;

import tools.refinery.store.dse.propagation.BoundPropagator;
import tools.refinery.store.dse.propagation.Propagator;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryBuilder;

import java.util.List;

public class RuleBasedPropagator implements Propagator {
	private final List<Rule> propagationRules;

	public RuleBasedPropagator(List<Rule> propagationRules) {
		this.propagationRules = propagationRules;
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		var queryBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		for (var propagationRule : propagationRules) {
			queryBuilder.query(propagationRule.getPrecondition());
		}
	}

	@Override
	public BoundPropagator bindToModel(Model model) {
		return new BoundRuleBasedPropagator(model, propagationRules);
	}
}
