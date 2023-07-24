/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import java.util.stream.Stream;

public interface StatefulAggregator<R, T> extends Aggregator<R, T> {
	StatefulAggregate<R, T> createEmptyAggregate();

	@Override
	default R aggregateStream(Stream<T> stream) {
		var accumulator = createEmptyAggregate();
		var iterator = stream.iterator();
		while (iterator.hasNext()) {
			var value = iterator.next();
			accumulator.add(value);
		}
		return accumulator.getResult();
	}

	@Override
	default R getEmptyResult() {
		return createEmptyAggregate().getResult();
	}
}
