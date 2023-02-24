package tools.refinery.store.query;

import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.TupleLike;

import java.util.Optional;
import java.util.stream.Stream;

public class EmptyResultSet implements ResultSet {
	@Override
	public boolean hasResult() {
		return false;
	}

	@Override
	public boolean hasResult(Tuple parameters) {
		return false;
	}

	@Override
	public Optional<TupleLike> oneResult() {
		return Optional.empty();
	}

	@Override
	public Optional<TupleLike> oneResult(Tuple parameters) {
		return Optional.empty();
	}

	@Override
	public Stream<TupleLike> allResults() {
		return Stream.of();
	}

	@Override
	public Stream<TupleLike> allResults(Tuple parameters) {
		return Stream.of();
	}

	@Override
	public int countResults() {
		return 0;
	}

	@Override
	public int countResults(Tuple parameters) {
		return 0;
	}
}
