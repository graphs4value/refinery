package tools.refinery.store.query;

import tools.refinery.store.tuple.TupleLike;

import java.util.stream.Stream;

public class EmptyResultSet implements ResultSet {
	@Override
	public boolean hasResult(TupleLike parameters) {
		return false;
	}

	@Override
	public Stream<TupleLike> allResults() {
		return Stream.of();
	}

	@Override
	public int countResults() {
		return 0;
	}
}
