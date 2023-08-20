/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.term.Aggregator;
import tools.refinery.store.query.term.AssignedValue;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FunctionalQuery<T> extends Query<T> {
	private final Class<T> type;

	FunctionalQuery(Dnf dnf, Class<T> type) {
		super(dnf);
		var parameters = dnf.getSymbolicParameters();
		int outputIndex = dnf.arity() - 1;
		for (int i = 0; i < outputIndex; i++) {
			var parameter = parameters.get(i);
			var parameterType = parameter.tryGetType();
			if (parameterType.isPresent()) {
				throw new InvalidQueryException("Expected parameter %s of %s to be a node variable, got %s instead"
						.formatted(parameter, dnf, parameterType.get().getName()));
			}
		}
		var outputParameter = parameters.get(outputIndex);
		var outputParameterType = outputParameter.tryGetType();
		if (outputParameterType.isEmpty() || !outputParameterType.get().equals(type)) {
			throw new InvalidQueryException("Expected parameter %s of %s to be %s, but got %s instead".formatted(
					outputParameter, dnf, type, outputParameterType.map(Class::getName).orElse("node")));
		}
		this.type = type;
	}

	@Override
	public int arity() {
		return getDnf().arity() - 1;
	}

	@Override
	public Class<T> valueType() {
		return type;
	}

	@Override
	public T defaultValue() {
		return null;
	}

	@Override
	protected FunctionalQuery<T> withDnfInternal(Dnf newDnf) {
		return newDnf.asFunction(type);
	}

	@Override
	public FunctionalQuery<T> withDnf(Dnf newDnf) {
		return (FunctionalQuery<T>) super.withDnf(newDnf);
	}

	public AssignedValue<T> call(List<NodeVariable> arguments) {
		return targetVariable -> {
			var argumentsWithTarget = new ArrayList<Variable>(arguments.size() + 1);
			argumentsWithTarget.addAll(arguments);
			argumentsWithTarget.add(targetVariable);
			return getDnf().call(CallPolarity.POSITIVE, argumentsWithTarget);
		};
	}

	public AssignedValue<T> call(NodeVariable... arguments) {
		return call(List.of(arguments));
	}

	public <R> AssignedValue<R> aggregate(Aggregator<R, T> aggregator, List<NodeVariable> arguments) {
		return targetVariable -> {
			var placeholderVariable = Variable.of(type);
			var argumentsWithPlaceholder = new ArrayList<Variable>(arguments.size() + 1);
			argumentsWithPlaceholder.addAll(arguments);
			argumentsWithPlaceholder.add(placeholderVariable);
			return getDnf()
					.aggregateBy(placeholderVariable, aggregator, argumentsWithPlaceholder)
					.toLiteral(targetVariable);
		};
	}

	public <R> AssignedValue<R> aggregate(Aggregator<R, T> aggregator, NodeVariable... arguments) {
		return aggregate(aggregator, List.of(arguments));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		FunctionalQuery<?> that = (FunctionalQuery<?>) o;
		return Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), type);
	}
}
