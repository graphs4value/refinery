package tools.refinery.store.reasoning.seed;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

public record UniformSeed<T>(int arity, T reducedValue) implements Seed<T> {
	public UniformSeed {
		if (arity < 0) {
			throw new IllegalArgumentException("Arity must not be negative");
		}
	}

	@Override
	public T get(Tuple key) {
		return reducedValue;
	}

	@Override
	public Cursor<Tuple, T> getCursor(T defaultValue, int nodeCount) {
		return null;
	}
}
