/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.actions;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class PutActionLiteral<T> extends AbstractActionLiteral {
	private final Symbol<T> symbol;
	private final List<NodeVariable> parameters;
	private final T value;

	public PutActionLiteral(Symbol<T> symbol, T value, List<NodeVariable> parameters) {
		if (symbol.arity() != parameters.size()) {
			throw new IllegalArgumentException("Expected %d parameters for symbol %s, got %d instead"
					.formatted(symbol.arity(), symbol, parameters.size()));
		}
		if (value != null && !symbol.valueType().isInstance(value)) {
			throw new IllegalArgumentException("Expected value of type %s for symbol %s, got %s of type %s instead"
					.formatted(symbol.valueType().getName(), symbol, value, value.getClass().getName()));
		}
		this.symbol = symbol;
		this.parameters = List.copyOf(parameters);
		this.value = value;
	}

	public Symbol<T> getSymbol() {
		return symbol;
	}

	public List<NodeVariable> getParameters() {
		return parameters;
	}

	public T getValue() {
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
		var interpretation = model.getInterpretation(symbol);
		return tuple -> {
			interpretation.put(tuple, value);
			return Tuple.of();
		};
	}
}
