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
import java.util.Set;

class BoundPropagationRule {
	private final Model model;
	private final Rule rule;
	private final ResultSet<Boolean> resultSet;
	private final BoundAction action;
	private final Set<Tuple> firedActivations;

	public BoundPropagationRule(Model model, Rule rule) {
		this.model = model;
		this.rule = rule;
		resultSet = model.getAdapter(ModelQueryAdapter.class).getResultSet(rule.getPrecondition());
		action = rule.createAction(model);
		if (rule.isDynamic()) {
			// Disable checking for stuck rules if an activation can be fired multiple times productively.
			firedActivations = null;
			return;
		}
		firedActivations = new HashSet<>();
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

	public boolean isDynamic() {
		return firedActivations == null;
	}

	public boolean canFire() {
		return resultSet.size() > 0;
	}

	public PropagationResult fireAll() {
		if (firedActivations != null && !firedActivations.isEmpty()) {
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
			if (firedActivations != null) {
				firedActivations.add(tuple);
			}
		}
		return PropagationResult.PROPAGATED;
	}
}
