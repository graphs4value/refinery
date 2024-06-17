/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.actions;

import tools.refinery.logic.term.NodeVariable;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class ConstantActionLiteral extends AbstractActionLiteral {
	private final NodeVariable variable;
	private final int nodeId;

	public ConstantActionLiteral(NodeVariable variable, int nodeId) {
		this.variable = variable;
		this.nodeId = nodeId;
	}

	public NodeVariable getVariable() {
		return variable;
	}

	public int getNodeId() {
		return nodeId;
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
		return ignoredTuple -> Tuple.of(nodeId);
	}
}
