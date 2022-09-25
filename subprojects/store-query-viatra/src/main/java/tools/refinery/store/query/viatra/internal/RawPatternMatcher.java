package tools.refinery.store.query.viatra.internal;

import org.eclipse.viatra.query.runtime.api.GenericPatternMatcher;
import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import org.eclipse.viatra.query.runtime.matchers.tuple.AbstractTuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;

import java.util.Optional;
import java.util.stream.Stream;

public class RawPatternMatcher extends GenericPatternMatcher {
	protected final Object[] empty;

	public RawPatternMatcher(GenericQuerySpecification<? extends GenericPatternMatcher> specification) {
		super(specification);
		this.empty = new Object[specification.getParameterNames().size()];
	}

	public boolean hasResult() {
		return hasResult(empty);
	}

	public boolean hasResult(Object[] parameters) {
		return this.backend.hasMatch(parameters);
	}

	public Optional<Object[]> oneResult() {
		return oneResult(empty);
	}

	public Optional<Object[]> oneResult(Object[] parameters) {
		Optional<Tuple> tuple = this.backend.getOneArbitraryMatch(parameters);
		return tuple.map(AbstractTuple::getElements);
	}

	public Stream<Object[]> allResults() {
		return allResults(empty);
	}

	public Stream<Object[]> allResults(Object[] parameters) {
		return this.backend.getAllMatches(parameters).map(AbstractTuple::getElements);
	}

	public int countResults() {
		return countResults(empty);
	}

	public int countResults(Object[] parameters) {
		return backend.countMatches(parameters);
	}
}
