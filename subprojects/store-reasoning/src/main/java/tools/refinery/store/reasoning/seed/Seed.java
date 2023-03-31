package tools.refinery.store.reasoning.seed;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

public interface Seed<T> {
	int arity();

	T reducedValue();

	T get(Tuple key);

	Cursor<Tuple, T> getCursor(T defaultValue, int nodeCount);
}
