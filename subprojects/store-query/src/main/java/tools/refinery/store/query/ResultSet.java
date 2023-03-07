package tools.refinery.store.query;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.tuple.TupleLike;

public non-sealed interface ResultSet<T> extends AnyResultSet {
	Query<T> getQuery();

	T get(TupleLike parameters);

	Cursor<TupleLike, T> getAll();
}
