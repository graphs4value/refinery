/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import tools.refinery.store.dse.transition.actions.BoundAction;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.OrderedResultSet;
import tools.refinery.store.query.resultset.OrderedResultSetImpl;
import tools.refinery.store.query.resultset.PriorityAgenda;
import tools.refinery.store.query.resultset.PriorityResultSet;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.tuple.Tuple;

public class Transformation {
	private final DecisionRule decisionRule;
	private final OrderedResultSet<Boolean> activations;
	private final BoundAction action;

	public Transformation(Model model, PriorityAgenda agenda, DecisionRule decisionRule) {
		this.decisionRule = decisionRule;
		var definition = decisionRule.rule();
		var precondition = definition.getPrecondition();
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var orderedResultSet = new OrderedResultSetImpl<>(queryEngine.getResultSet(precondition));
		activations = new PriorityResultSet<>(orderedResultSet, decisionRule.priority(), agenda);
		action = definition.createAction(model);
	}

	public DecisionRule getDefinition() {
		return decisionRule;
	}

	public ResultSet<Boolean> getAllActivationsAsResultSet() {
		return activations;
	}

	public Tuple getActivation(int index) {
		return activations.getKey(index);
	}

	public boolean fireActivation(Tuple activation) {
		return action.fire(activation);
	}
}
