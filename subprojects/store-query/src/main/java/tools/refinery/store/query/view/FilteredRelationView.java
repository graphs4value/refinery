package tools.refinery.store.query.view;

import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.representation.Symbol;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class FilteredRelationView<T> extends TuplePreservingRelationView<T> {
	private final BiPredicate<Tuple, T> predicate;

	public FilteredRelationView(Symbol<T> symbol, String name, BiPredicate<Tuple, T> predicate) {
		super(symbol, name);
		this.predicate = predicate;
	}

	public FilteredRelationView(Symbol<T> symbol, BiPredicate<Tuple, T> predicate) {
		super(symbol);
		this.predicate = predicate;
	}

	public FilteredRelationView(Symbol<T> symbol, String name, Predicate<T> predicate) {
		this(symbol, name, (k, v) -> predicate.test(v));
	}

	public FilteredRelationView(Symbol<T> symbol, Predicate<T> predicate) {
		this(symbol, (k, v) -> predicate.test(v));
	}

	@Override
	public boolean filter(Tuple key, T value) {
		return this.predicate.test(key, value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		FilteredRelationView<?> that = (FilteredRelationView<?>) o;
		return Objects.equals(predicate, that.predicate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), predicate);
	}
}
