package tools.refinery.store.query.view;

import tools.refinery.store.map.CursorAsIterator;
import tools.refinery.store.model.Model;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a view of a {@link Symbol} that can be queried.
 *
 * @param <T>
 * @author Oszkar Semerath
 */
public abstract non-sealed class RelationView<T> implements AnyRelationView {
	private final Symbol<T> symbol;

	private final String name;

	protected RelationView(Symbol<T> symbol, String name) {
		this.symbol = symbol;
		this.name = name;
	}

	protected RelationView(Symbol<T> representation) {
		this(representation, UUID.randomUUID().toString());
	}

	@Override
	public Symbol<T> getSymbol() {
		return symbol;
	}

	@Override
	public String name() {
		return symbol.name() + "#" + name;
	}

	public abstract boolean filter(Tuple key, T value);

	public abstract Object[] forwardMap(Tuple key, T value);

	@Override
	public Iterable<Object[]> getAll(Model model) {
		return (() -> new CursorAsIterator<>(model.getInterpretation(symbol).getAll(), this::forwardMap, this::filter));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RelationView<?> that = (RelationView<?>) o;
		return Objects.equals(symbol, that.symbol) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(symbol, name);
	}
}
