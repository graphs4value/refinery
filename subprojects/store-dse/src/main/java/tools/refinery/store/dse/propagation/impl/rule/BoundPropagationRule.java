/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation.impl.rule;

import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.actions.BoundAction;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelListener;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.tuple.Tuple;

import java.util.HashSet;

class BoundPropagationRule {
	private final Model model;
	private final Rule rule;
	private final ResultSet<Boolean> resultSet;
	private final BoundAction action;
	private final HashSet<Tuple> firedActivations = new HashSet<>();

	public BoundPropagationRule(Model model, Rule rule) {
		this.model = model;
		this.rule = rule;
		resultSet = model.getAdapter(ModelQueryAdapter.class).getResultSet(rule.getPrecondition());
		action = rule.createAction(model);
		model.addListener(new ModelListener() {
			@Override
			public void afterRestore() {
				firedActivations.clear();
			}
		});
		resultSet.addListener((key, fromValue, toValue) -> {
			if (Boolean.FALSE.equals(toValue)) {
				firedActivations.remove(key);
			}
		});
	}

	public PropagationResult fireAll() {
		if (!firedActivations.isEmpty()) {
			return new PropagationRejectedResult(rule, "Propagation rule '%s' got stuck.".formatted(rule.getName()),
					true);
		}
		if (resultSet.size() == 0) {
			return PropagationResult.UNCHANGED;
		}
		var cursor = resultSet.getAll();
		while (cursor.move()) {
			model.checkCancelled();
			var tuple = cursor.getKey();
			var result = action.fire(tuple);
			if (!result) {
				return new PropagationRejectedResult(rule, "Propagation rule '%s' failed.".formatted(rule.getName()));
			}
			firedActivations.add(tuple);
		}
		return PropagationResult.PROPAGATED;
	}
}
