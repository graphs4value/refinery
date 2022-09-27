package tools.refinery.store.query;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.TupleLike;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface QueryableModel extends Model {
	Set<DNFPredicate> getPredicates();

	boolean hasChanges();

	void flushChanges();

	boolean hasResult(DNFPredicate predicate);

	boolean hasResult(DNFPredicate predicate, Tuple parameters);

	Optional<TupleLike> oneResult(DNFPredicate predicate);

	Optional<TupleLike> oneResult(DNFPredicate predicate, Tuple parameters);

	Stream<TupleLike> allResults(DNFPredicate predicate);

	Stream<TupleLike> allResults(DNFPredicate predicate, Tuple parameters);

	int countResults(DNFPredicate predicate);

	int countResults(DNFPredicate predicate, Tuple parameters);
}
