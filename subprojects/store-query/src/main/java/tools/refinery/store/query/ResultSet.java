package tools.refinery.store.query;

import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.TupleLike;

import java.util.Optional;
import java.util.stream.Stream;

public interface ResultSet {
	boolean hasResult();

	boolean hasResult(Tuple parameters);

	Optional<TupleLike> oneResult();

	Optional<TupleLike> oneResult(Tuple parameters);

	Stream<TupleLike> allResults();

	Stream<TupleLike> allResults(Tuple parameters);

	int countResults();

	int countResults(Tuple parameters);
}
