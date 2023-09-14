/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.resultset;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.tuple.Tuple;

public record EmptyResultSet<T>(ModelQueryAdapter adapter, Query<T> query) implements ResultSet<T> {
	@Override
	public ModelQueryAdapter getAdapter() {
		return adapter;
	}

	@Override
	public Query<T> getCanonicalQuery() {
		return query;
	}

	@Override
	public T get(Tuple parameters) {
		return query.defaultValue();
	}

	@Override
	public Cursor<Tuple, T> getAll() {
		return Cursors.empty();
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public void addListener(ResultSetListener<T> listener) {
		// No need to store the listener, because the empty result set will never change.
	}

	@Override
	public void removeListener(ResultSetListener<T> listener) {
		// No need to remove the listener, because we never stored it.
	}
}
