/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.actions;

import tools.refinery.store.dse.transition.actions.AbstractActionLiteral;
import tools.refinery.store.dse.transition.actions.BoundActionLiteral;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.reasoning.ReasoningAdapter;

import java.util.List;

public class FocusActionLiteral extends AbstractActionLiteral {
	private final NodeVariable parentNode;
	private final NodeVariable childNode;

	public FocusActionLiteral(NodeVariable parentNode, NodeVariable childNode) {
		this.parentNode = parentNode;
		this.childNode = childNode;
	}

	public NodeVariable getParentNode() {
		return parentNode;
	}

	public NodeVariable getChildNode() {
		return childNode;
	}

	@Override
	public List<NodeVariable> getInputVariables() {
		return List.of(parentNode);
	}

	@Override
	public List<NodeVariable> getOutputVariables() {
		return List.of(childNode);
	}

	@Override
	public BoundActionLiteral bindToModel(Model model) {
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		return tuple -> reasoningAdapter.focus(tuple.get(0));
	}
}
