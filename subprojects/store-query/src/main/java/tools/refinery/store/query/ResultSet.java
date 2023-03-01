package tools.refinery.store.query;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.TupleLike;

public interface ResultSet {
	default boolean hasResult() {
		return countResults() > 0;
	}

	boolean hasResult(TupleLike parameters);

	Cursor<TupleLike, Boolean> allResults();

	int countResults();
}
