/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.pquery;

import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;
import tools.refinery.store.query.term.StatefulAggregate;
import tools.refinery.store.query.term.StatefulAggregator;

import java.util.stream.Stream;

record StatefulMultisetAggregator<R, T>(StatefulAggregator<R, T> aggregator)
	implements IMultisetAggregationOperator<T, StatefulAggregate<R, T>, R> {
	@Override
	public String getShortDescription() {
		return getName();
	}

	@Override
	public String getName() {
		return aggregator.toString();
	}

	@Override
	public StatefulAggregate<R, T> createNeutral() {
		return aggregator.createEmptyAggregate();
	}

	@Override
	public boolean isNeutral(StatefulAggregate<R, T> result) {
		return result.isEmpty();
	}

	@Override
	public StatefulAggregate<R, T> update(StatefulAggregate<R, T> oldResult, T updateValue, boolean isInsertion) {
		if (isInsertion) {
			oldResult.add(updateValue);
		} else {
			oldResult.remove(updateValue);
		}
		return oldResult;
	}

	@Override
	public R getAggregate(StatefulAggregate<R, T> result) {
		return result.getResult();
	}

	@Override
	public R aggregateStream(Stream<T> stream) {
		return aggregator.aggregateStream(stream);
	}

	@Override
	public StatefulAggregate<R, T> clone(StatefulAggregate<R, T> original) {
		return original.deepCopy();
	}

	@Override
	public boolean contains(T value, StatefulAggregate<R, T> accumulator) {
		return accumulator.contains(value);
	}
}
