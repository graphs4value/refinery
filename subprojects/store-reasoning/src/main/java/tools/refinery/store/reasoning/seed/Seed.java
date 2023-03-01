package tools.refinery.store.reasoning.translator;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

public interface Seed<T> {
	int arity();

	T getReducedValue();

	T get(Tuple key);

	Cursor<Tuple, T> getCursor(T defaultValue, int nodeCount);
}
