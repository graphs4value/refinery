package tools.refinery.store.query.internal;

import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.viatra.query.runtime.api.GenericPatternMatcher;
import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.AbstractTuple;

public class RawPatternMatcher extends GenericPatternMatcher implements PredicateResult{
	
	protected final Object[] empty;
	
	public RawPatternMatcher(GenericQuerySpecification<? extends GenericPatternMatcher> specification) {
		super(specification);
		this.empty = new Object[specification.getParameterNames().size()];
	}
	
	@Override
	public boolean hasResult() {
		return hasResult(empty);
	}
	@Override
	public boolean hasResult(Object[] parameters) {
		return this.backend.hasMatch(parameters);
	}
	@Override
	public Optional<Object[]> oneResult() {
		return oneResult(empty);
	}
	@Override
	public Optional<Object[]> oneResult(Object[] parameters) {
		Optional<Tuple> tuple = this.backend.getOneArbitraryMatch(parameters);
		if(tuple.isPresent()) {
			return Optional.of(tuple.get().getElements());
		} else {
			return Optional.empty();
		}
	}
	@Override
	public Stream<Object[]> allResults() {
		return allResults(empty);
	}
	@Override
	public Stream<Object[]> allResults(Object[] parameters) {
		return this.backend.getAllMatches(parameters).map(AbstractTuple::getElements);
	}
	@Override
	public int countResults() {
		return countResults(empty);
	}
	@Override
	public int countResults(Object[] parameters) {
		return backend.countMatches(parameters);
	}
}
