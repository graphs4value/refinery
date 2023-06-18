/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.literal.*;
import tools.refinery.store.query.term.*;

import java.util.List;

public interface Constraint {
	String name();

	List<Parameter> getParameters();

	default int arity() {
		return getParameters().size();
	}

	default boolean invalidIndex(int i) {
		return i < 0 || i >= arity();
	}

	default Reduction getReduction() {
		return Reduction.NOT_REDUCIBLE;
	}

	default boolean equals(LiteralEqualityHelper helper, Constraint other) {
		return equals(other);
	}

	default String toReferenceString() {
		return name();
	}

	default CallLiteral call(CallPolarity polarity, List<Variable> arguments) {
		return new CallLiteral(polarity, this, arguments);
	}

	default CallLiteral call(CallPolarity polarity, Variable... arguments) {
		return call(polarity, List.of(arguments));
	}

	default CallLiteral call(Variable... arguments) {
		return call(CallPolarity.POSITIVE, arguments);
	}

	default CallLiteral callTransitive(NodeVariable left, NodeVariable right) {
		return call(CallPolarity.TRANSITIVE, List.of(left, right));
	}

	default AssignedValue<Integer> count(List<Variable> arguments) {
		return targetVariable -> new CountLiteral(targetVariable, this, arguments);
	}

	default AssignedValue<Integer> count(Variable... arguments) {
		return count(List.of(arguments));
	}

	default <R, T> AssignedValue<R> aggregateBy(DataVariable<T> inputVariable, Aggregator<R, T> aggregator,
												List<Variable> arguments) {
		return targetVariable -> new AggregationLiteral<>(targetVariable, aggregator, inputVariable, this, arguments);
	}

	default <R, T> AssignedValue<R> aggregateBy(DataVariable<T> inputVariable, Aggregator<R, T> aggregator,
												Variable... arguments) {
		return aggregateBy(inputVariable, aggregator, List.of(arguments));
	}
}
