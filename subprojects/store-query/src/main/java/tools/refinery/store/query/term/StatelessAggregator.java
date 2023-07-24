/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import java.util.stream.Stream;

public interface StatelessAggregator<R, T> extends Aggregator<R, T> {
	R add(R current, T value);

	R remove(R current, T value);

	@Override
	default R aggregateStream(Stream<T> stream) {
		var accumulator = getEmptyResult();
		var iterator = stream.iterator();
		while (iterator.hasNext()) {
			var value = iterator.next();
			accumulator = add(accumulator, value);
		}
		return accumulator;
	}
}
