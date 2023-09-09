/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation.impl.rule;

import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.actions.BoundAction;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.resultset.ResultSet;

class BoundPropagationRule {
	private final ResultSet<Boolean> resultSet;
	private final BoundAction action;

	public BoundPropagationRule(Model model, Rule rule) {
		resultSet = model.getAdapter(ModelQueryAdapter.class).getResultSet(rule.getPrecondition());
		action = rule.createAction(model);
	}

	public PropagationResult fireAll() {
		if (resultSet.size() == 0) {
			return PropagationResult.UNCHANGED;
		}
		var cursor = resultSet.getAll();
		while (cursor.move()) {
			var result = action.fire(cursor.getKey());
			if (!result) {
				return PropagationResult.REJECTED;
			}
		}
		return PropagationResult.PROPAGATED;
	}
}
