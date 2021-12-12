package tools.refinery.store.query.internal;

import java.util.Optional;
import java.util.stream.Stream;

public interface PredicateResult {

	boolean hasResult();

	boolean hasResult(Object[] parameters);

	Optional<Object[]> oneResult();

	Optional<Object[]> oneResult(Object[] parameters);

	Stream<Object[]> allResults();

	Stream<Object[]> allResults(Object[] parameters);

	int countResults();

	int countResults(Object[] parameters);

}