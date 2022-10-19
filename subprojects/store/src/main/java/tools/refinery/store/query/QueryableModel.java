package tools.refinery.store.query;

import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.TupleLike;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface QueryableModel extends Model {
	Set<DNF> getPredicates();

	boolean hasChanges();

	void flushChanges();

	boolean hasResult(DNF predicate);

	boolean hasResult(DNF predicate, Tuple parameters);

	Optional<TupleLike> oneResult(DNF predicate);

	Optional<TupleLike> oneResult(DNF predicate, Tuple parameters);

	Stream<TupleLike> allResults(DNF predicate);

	Stream<TupleLike> allResults(DNF predicate, Tuple parameters);

	int countResults(DNF predicate);

	int countResults(DNF predicate, Tuple parameters);
}
