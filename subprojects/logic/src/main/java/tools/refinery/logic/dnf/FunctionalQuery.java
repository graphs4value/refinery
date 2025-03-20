/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf;

import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.term.*;

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

	public <R> Term<R> aggregate(Aggregator<R, T> aggregator, List<NodeVariable> arguments) {
		var placeholderVariable = Variable.of(type);
		var argumentsWithPlaceholder = new ArrayList<Variable>(arguments.size() + 1);
		argumentsWithPlaceholder.addAll(arguments);
		argumentsWithPlaceholder.add(placeholderVariable);
		return getDnf().aggregateBy(placeholderVariable, aggregator, argumentsWithPlaceholder);
	}

	public <R> Term<R> aggregate(Aggregator<R, T> aggregator, NodeVariable... arguments) {
		return aggregate(aggregator, List.of(arguments));
	}

	public Term<T> leftJoin(T defaultValue, List<NodeVariable> arguments) {
		var placeholderVariable = Variable.of(type);
		var argumentsWithPlaceholder = new ArrayList<Variable>(arguments.size() + 1);
		argumentsWithPlaceholder.addAll(arguments);
		argumentsWithPlaceholder.add(placeholderVariable);
		return new LeftJoinTerm<>(placeholderVariable, defaultValue, getDnf(), argumentsWithPlaceholder);
	}

	public Term<T> leftJoin(T defaultValue, NodeVariable... arguments) {
		return leftJoin(defaultValue, List.of(arguments));
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
