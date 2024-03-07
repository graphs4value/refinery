/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.logic.term.*;
import tools.refinery.store.representation.Symbol;

import java.util.ArrayList;
import java.util.List;

public final class FunctionView<T> extends AbstractFunctionView<T> {
	public FunctionView(Symbol<T> symbol, String name) {
		super(symbol, name, new Parameter(symbol.valueType(), ParameterDirection.OUT));
	}

	public FunctionView(Symbol<T> symbol) {
		this(symbol, "function");
	}

	public <R> AssignedValue<R> aggregate(Aggregator<R, T> aggregator, List<NodeVariable> arguments) {
		return targetVariable -> {
			var placeholderVariable = Variable.of(getSymbol().valueType());
			var argumentsWithPlaceholder = new ArrayList<Variable>(arguments.size() + 1);
			argumentsWithPlaceholder.addAll(arguments);
			argumentsWithPlaceholder.add(placeholderVariable);
			return aggregateBy(placeholderVariable, aggregator, argumentsWithPlaceholder).toLiteral(targetVariable);
		};
	}

	public <R> AssignedValue<R> aggregate(Aggregator<R, T> aggregator, NodeVariable... arguments) {
		return aggregate(aggregator, List.of(arguments));
	}

	public AssignedValue<T> leftJoin(T defaultValue, List<NodeVariable> arguments) {
		return targetVariable -> {
			var placeholderVariable = Variable.of(getSymbol().valueType());
			var argumentsWithPlaceholder = new ArrayList<Variable>(arguments.size() + 1);
			argumentsWithPlaceholder.addAll(arguments);
			argumentsWithPlaceholder.add(placeholderVariable);
			return leftJoinBy(placeholderVariable, defaultValue, argumentsWithPlaceholder).toLiteral(targetVariable);
		};
	}

	public AssignedValue<T> leftJoin(T defaultValue, NodeVariable... arguments) {
		return leftJoin(defaultValue, List.of(arguments));
	}
}
