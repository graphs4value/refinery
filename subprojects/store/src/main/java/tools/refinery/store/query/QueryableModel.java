package tools.refinery.store.query;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.building.DNFPredicate;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface QueryableModel extends Model{
	Set<DNFPredicate> getPredicates();

	boolean hasChanges();

	void flushChanges();

	boolean hasResult(DNFPredicate predicate);

	boolean hasResult(DNFPredicate predicate, Object[] parameters);

	Optional<Object[]> oneResult(DNFPredicate predicate);

	Optional<Object[]> oneResult(DNFPredicate predicate, Object[] parameters);

	Stream<Object[]> allResults(DNFPredicate predicate);

	Stream<Object[]> allResults(DNFPredicate predicate, Object[] parameters);

	int countResults(DNFPredicate predicate);

	int countResults(DNFPredicate predicate, Object[] parameters);
}
