package tools.refinery.store.query;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.tuple.TupleLike;

public class EmptyResultSet implements ResultSet {
	@Override
	public boolean hasResult(TupleLike parameters) {
		return false;
	}

	@Override
	public Cursor<TupleLike, Boolean> allResults() {
		return Cursors.empty();
	}

	@Override
	public int countResults() {
		return 0;
	}
}
