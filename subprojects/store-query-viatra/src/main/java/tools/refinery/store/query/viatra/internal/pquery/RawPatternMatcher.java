package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.api.GenericPatternMatcher;
import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import tools.refinery.store.query.ResultSet;
import tools.refinery.store.query.viatra.ViatraTupleLike;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.TupleLike;

import java.util.Optional;
import java.util.stream.Stream;

public class RawPatternMatcher extends GenericPatternMatcher implements ResultSet {
	protected final Object[] empty;

	public RawPatternMatcher(GenericQuerySpecification<? extends GenericPatternMatcher> specification) {
		super(specification);
		empty = new Object[specification.getParameterNames().size()];
	}

	@Override
	public boolean hasResult() {
		return backend.hasMatch(empty);
	}

	@Override
	public boolean hasResult(Tuple parameters) {
		return backend.hasMatch(toParametersArray(parameters));
	}

	@Override
	public Optional<TupleLike> oneResult() {
		return backend.getOneArbitraryMatch(empty).map(ViatraTupleLike::new);
	}

	@Override
	public Optional<TupleLike> oneResult(Tuple parameters) {
		return backend.getOneArbitraryMatch(toParametersArray(parameters)).map(ViatraTupleLike::new);
	}

	@Override
	public Stream<TupleLike> allResults() {
		return backend.getAllMatches(empty).map(ViatraTupleLike::new);
	}

	@Override
	public Stream<TupleLike> allResults(Tuple parameters) {
		return backend.getAllMatches(toParametersArray(parameters)).map(ViatraTupleLike::new);
	}

	@Override
	public int countResults() {
		return backend.countMatches(empty);
	}

	@Override
	public int countResults(Tuple parameters) {
		return backend.countMatches(toParametersArray(parameters));
	}

	private Object[] toParametersArray(Tuple tuple) {
		int size = tuple.getSize();
		var array = new Object[tuple.getSize()];
		for (int i = 0; i < size; i++) {
			var value = tuple.get(i);
			if (value >= 0) {
				array[i] = Tuple.of(value);
			}
		}
		return array;
	}
}
