/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import java.util.stream.Stream;

public interface Aggregator<R, T> {
	Class<R> getResultType();

	Class<T> getInputType();

	R aggregateStream(Stream<T> stream);

	R getEmptyResult();
}
