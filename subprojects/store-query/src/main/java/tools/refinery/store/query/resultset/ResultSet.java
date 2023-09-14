/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.resultset;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.tuple.Tuple;

public non-sealed interface ResultSet<T> extends AnyResultSet {
	Query<T> getCanonicalQuery();

	T get(Tuple parameters);

	Cursor<Tuple, T> getAll();

	void addListener(ResultSetListener<T> listener);

	void removeListener(ResultSetListener<T> listener);
}
