/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.term.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FunctionalQuery<T> implements Query<T> {
	private final Dnf dnf;
	private final Class<T> type;

	FunctionalQuery(Dnf dnf, Class<T> type) {
		var parameters = dnf.getParameters();
		int outputIndex = dnf.arity() - 1;
		for (int i = 0; i < outputIndex; i++) {
			var parameter = parameters.get(i);
			if (!(parameter instanceof NodeVariable)) {
				throw new IllegalArgumentException("Expected parameter %s of %s to be of sort %s, but got %s instead"
						.formatted(parameter, dnf, NodeSort.INSTANCE, parameter.getSort()));
			}
		}
		var outputParameter = parameters.get(outputIndex);
		if (!(outputParameter instanceof DataVariable<?> dataOutputParameter) ||
				!dataOutputParameter.getType().equals(type)) {
			throw new IllegalArgumentException("Expected parameter %s of %s to be of sort %s, but got %s instead"
					.formatted(outputParameter, dnf, type, outputParameter.getSort()));
		}
		this.dnf = dnf;
		this.type = type;
	}

	@Override
	public String name() {
		return dnf.name();
	}

	@Override
	public int arity() {
		return dnf.arity() - 1;
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
	public Dnf getDnf() {
		return dnf;
	}

	public AssignedValue<T> call(List<NodeVariable> arguments) {
		return targetVariable -> {
			var argumentsWithTarget = new ArrayList<Variable>(arguments.size() + 1);
			argumentsWithTarget.addAll(arguments);
			argumentsWithTarget.add(targetVariable);
			return dnf.call(CallPolarity.POSITIVE, argumentsWithTarget);
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
			return dnf.aggregate(placeholderVariable, aggregator, argumentsWithPlaceholder).toLiteral(targetVariable);
		};
	}

	public <R> AssignedValue<R> aggregate(Aggregator<R, T> aggregator, NodeVariable... arguments) {
		return aggregate(aggregator, List.of(arguments));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FunctionalQuery<?> that = (FunctionalQuery<?>) o;
		return dnf.equals(that.dnf) && type.equals(that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dnf, type);
	}

	@Override
	public String toString() {
		return dnf.toString();
	}
}
