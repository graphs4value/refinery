/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.resultset;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.tuple.Tuple;

/**
 * A ResultSet that always returns true for the empty tuple.
 *
 * @param adapter The ModelQueryAdapter for this ResultSet.
 * @param query   The query that this ResultSet should always return true for. Must be a query of arity 0.
 */
public record AlwaysTrueResultSet(ModelQueryAdapter adapter, RelationalQuery query) implements ResultSet<Boolean> {
	public AlwaysTrueResultSet {
		if (query.arity() != 0) {
			throw new IllegalArgumentException("AlwaysTrueResultSet can only be used with queries of arity 0");
		}
	}

	@Override
	public ModelQueryAdapter getAdapter() {
		return adapter;
	}

	@Override
	public Query<Boolean> getCanonicalQuery() {
		return query;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public Boolean get(Tuple parameters) {
		return true;
	}

	@Override
	public Cursor<Tuple, Boolean> getAll() {
		return new Cursor<>() {
			private boolean terminated;
			private Tuple key;

			@Override
			public Tuple getKey() {
				return key;
			}

			@Override
			public Boolean getValue() {
				return true;
			}

			@Override
			public boolean isTerminated() {
				return terminated;
			}

			@Override
			public boolean move() {
				if (terminated) {
					return false;
				}
				if (key == null) {
					key = Tuple.of();
					return true;
				}
				key = null;
				terminated = true;
				return false;
			}
		};
	}

	@Override
	public void addListener(ResultSetListener<Boolean> listener) {
		// No need to store the listener, because the singleton result set will never change.
	}

	@Override
	public void removeListener(ResultSetListener<Boolean> listener) {
		// No need to remove the listener, because we never stored it.
	}
}
