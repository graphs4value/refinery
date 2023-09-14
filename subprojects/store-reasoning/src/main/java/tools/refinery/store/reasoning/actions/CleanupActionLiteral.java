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
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class CleanupActionLiteral extends AbstractActionLiteral {
	private final NodeVariable node;

	public CleanupActionLiteral(NodeVariable node) {
		this.node = node;
	}

	public NodeVariable getNode() {
		return node;
	}

	@Override
	public List<NodeVariable> getInputVariables() {
		return List.of(node);
	}

	@Override
	public List<NodeVariable> getOutputVariables() {
		return List.of();
	}

	@Override
	public BoundActionLiteral bindToModel(Model model) {
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		return tuple -> reasoningAdapter.cleanup(tuple.get(0)) ? Tuple.of() : null;
	}
}
