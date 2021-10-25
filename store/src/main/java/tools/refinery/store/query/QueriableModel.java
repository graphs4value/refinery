package tools.refinery.store.query;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.building.DNFPredicate;

public interface QueriableModel extends Model{
	Set<DNFPredicate> getPredicates();
	
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
