/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import tools.refinery.store.dse.transition.actions.BoundAction;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.resultset.OrderedResultSet;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.tuple.Tuple;

public class Transformation {
	private final Rule definition;
	private final OrderedResultSet<Boolean> activations;
	private final BoundAction action;

	public Transformation(Model model, Rule definition) {
		this.definition = definition;
		var precondition = definition.getPrecondition();
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		activations = new OrderedResultSet<>(queryEngine.getResultSet(precondition));
		action = definition.createAction(model);
	}

	public Rule getDefinition() {
		return definition;
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
