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
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class MergeActionLiteral<A, C> extends AbstractActionLiteral {
	private final PartialSymbol<A, C> partialSymbol;
	private final List<NodeVariable> parameters;
	private final A value;

	public MergeActionLiteral(PartialSymbol<A, C> partialSymbol, A value, List<NodeVariable> parameters) {
		if (partialSymbol.arity() != parameters.size()) {
			throw new IllegalArgumentException("Expected %d parameters for partial symbol %s, got %d instead"
					.formatted(partialSymbol.arity(), partialSymbol, parameters.size()));
		}
		this.partialSymbol = partialSymbol;
		this.parameters = parameters;
		this.value = value;
	}

	public PartialSymbol<A, C> getPartialSymbol() {
		return partialSymbol;
	}

	public List<NodeVariable> getParameters() {
		return parameters;
	}

	public A getValue() {
		return value;
	}

	@Override
	public List<NodeVariable> getInputVariables() {
		return getParameters();
	}

	@Override
	public List<NodeVariable> getOutputVariables() {
		return List.of();
	}

	@Override
	public BoundActionLiteral bindToModel(Model model) {
		var refiner = model.getAdapter(ReasoningAdapter.class).getRefiner(partialSymbol);
		return tuple -> refiner.merge(tuple, value) ? Tuple.of() : null;
	}
}
