package tools.refinery.store.query.view;

import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public class KeyOnlyRelationView<T> extends TuplePreservingRelationView<T> {
	public static final String VIEW_NAME = "key";

	private final T defaultValue;

	public KeyOnlyRelationView(Symbol<T> symbol) {
		super(symbol, VIEW_NAME);
		defaultValue = symbol.defaultValue();
	}

	@Override
	public boolean filter(Tuple key, T value) {
		return !Objects.equals(value, defaultValue);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		KeyOnlyRelationView<?> that = (KeyOnlyRelationView<?>) o;
		return Objects.equals(defaultValue, that.defaultValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), defaultValue);
	}
}
