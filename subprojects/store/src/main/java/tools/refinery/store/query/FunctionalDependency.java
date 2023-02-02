package tools.refinery.store.query;

import java.util.HashSet;
import java.util.Set;

public record FunctionalDependency<T>(Set<T> forEach, Set<T> unique) {
	public FunctionalDependency {
		var uniqueForEach = new HashSet<>(unique);
		uniqueForEach.retainAll(forEach);
		if (!uniqueForEach.isEmpty()) {
			throw new IllegalArgumentException("Variables %s appear on both sides of the functional dependency"
					.formatted(uniqueForEach));
		}
	}
}
