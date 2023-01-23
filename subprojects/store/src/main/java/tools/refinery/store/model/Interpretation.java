package tools.refinery.store.model;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

public non-sealed interface Interpretation<T> extends AnyInterpretation {
	@Override
	Symbol<T> getSymbol();

	T get(Tuple key);

	Cursor<Tuple, T> getAll();

	T put(Tuple key, T value);

	void putAll(Cursor<Tuple, T> cursor);

	DiffCursor<Tuple, T> getDiffCursor(long to);

	void addListener(InterpretationListener<T> listener, boolean alsoWhenRestoring);

	void removeListener(InterpretationListener<T> listener);
}
