/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.resultset;

import tools.refinery.store.tuple.Tuple;

@FunctionalInterface
public interface ResultSetListener<T> {
	void put(Tuple key, T fromValue, T toValue);
}
