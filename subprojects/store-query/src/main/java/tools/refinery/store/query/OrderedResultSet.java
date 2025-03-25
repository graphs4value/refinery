/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query;

import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.tuple.Tuple;

public interface OrderedResultSet<T> extends AutoCloseable, ResultSet<T> {
	Tuple getKey(int index);
}
