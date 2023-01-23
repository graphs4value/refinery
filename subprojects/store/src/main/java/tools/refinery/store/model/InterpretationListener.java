package tools.refinery.store.model;

import tools.refinery.store.tuple.Tuple;

public interface InterpretationListener<T> {
	void put(Tuple key, T fromValue, T toValue, boolean restoring);
}
