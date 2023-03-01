package tools.refinery.store.query;

import tools.refinery.store.tuple.TupleLike;

import java.util.stream.Stream;

public interface ResultSet {
	default boolean hasResult() {
		return countResults() > 0;
	}

	boolean hasResult(TupleLike parameters);

	Stream<TupleLike> allResults();

	int countResults();
}
