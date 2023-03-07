package tools.refinery.store.query;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.tuple.TupleLike;

public record EmptyResultSet<T>(ModelQueryAdapter adapter, Query<T> query) implements ResultSet<T> {
	@Override
	public ModelQueryAdapter getAdapter() {
		return adapter;
	}

	@Override
	public Query<T> getQuery() {
		return query;
	}

	@Override
	public T get(TupleLike parameters) {
		return query.defaultValue();
	}


	@Override
	public Cursor<TupleLike, T> getAll() {
		return Cursors.empty();
	}

	@Override
	public int size() {
		return 0;
	}
}
