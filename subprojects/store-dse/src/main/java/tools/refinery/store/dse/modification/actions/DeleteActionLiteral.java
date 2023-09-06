/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.modification.actions;

import tools.refinery.store.dse.modification.DanglingEdges;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.transition.actions.AbstractActionLiteral;
import tools.refinery.store.dse.transition.actions.BoundActionLiteral;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class DeleteActionLiteral extends AbstractActionLiteral {
	private final NodeVariable variable;
	private final DanglingEdges danglingEdges;

	public DeleteActionLiteral(NodeVariable variable, DanglingEdges danglingEdges) {

		this.variable = variable;
		this.danglingEdges = danglingEdges;
	}

	public NodeVariable getVariable() {
		return variable;
	}

	public DanglingEdges getDanglingEdges() {
		return danglingEdges;
	}

	@Override
	public List<NodeVariable> getInputVariables() {
		return List.of(variable);
	}

	@Override
	public List<NodeVariable> getOutputVariables() {
		return List.of();
	}

	@Override
	public BoundActionLiteral bindToModel(Model model) {
		var adapter = model.getAdapter(ModificationAdapter.class);
		return tuple -> adapter.deleteObject(tuple, danglingEdges) ? Tuple.of() : null;
	}
}
