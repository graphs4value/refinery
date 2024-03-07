/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.tests;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.term.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record FakeFunctionView<T>(String name, int keyArity, Class<T> valueType) implements Constraint {
	@Override
	public int arity() {
		return keyArity + 1;
	}

	@Override
	public List<Parameter> getParameters() {
		var parameters = new Parameter[keyArity + 1];
		Arrays.fill(parameters, Parameter.NODE_OUT);
		parameters[keyArity] = new Parameter(valueType, ParameterDirection.OUT);
		return List.of(parameters);
	}

	public <R> AssignedValue<R> aggregate(Aggregator<R, T> aggregator, List<NodeVariable> arguments) {
		return targetVariable -> {
			var placeholderVariable = Variable.of(valueType);
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
			var placeholderVariable = Variable.of(valueType);
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
