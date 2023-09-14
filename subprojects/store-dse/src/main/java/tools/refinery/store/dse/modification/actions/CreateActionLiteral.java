/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.modification.actions;

import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.transition.actions.AbstractActionLiteral;
import tools.refinery.store.dse.transition.actions.BoundActionLiteral;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.term.NodeVariable;

import java.util.List;

public class CreateActionLiteral extends AbstractActionLiteral {
	private final NodeVariable variable;

	public CreateActionLiteral(NodeVariable variable) {

		this.variable = variable;
	}

	public NodeVariable getVariable() {
		return variable;
	}

	@Override
	public List<NodeVariable> getInputVariables() {
		return List.of();
	}

	@Override
	public List<NodeVariable> getOutputVariables() {
		return List.of(variable);
	}

	@Override
	public BoundActionLiteral bindToModel(Model model) {
		var adapter = model.getAdapter(ModificationAdapter.class);
		return ignoredTuple -> adapter.createObject();
	}
}
