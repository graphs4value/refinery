/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
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
public abstract non-sealed class SymbolView<T> implements AnySymbolView {
	private final Symbol<T> symbol;
	private final String viewName;

	protected SymbolView(Symbol<T> symbol, String viewName) {
		this.symbol = symbol;
		this.viewName = viewName;
	}

	protected SymbolView(Symbol<T> representation) {
		this(representation, UUID.randomUUID().toString());
	}

	@Override
	public Symbol<T> getSymbol() {
		return symbol;
	}

	@Override
	public String getViewName() {
		return viewName;
	}

	@Override
	public String name() {
		return symbol.name() + "#" + viewName;
	}

	public final boolean filter(Tuple key, T value) {
		return !Objects.equals(symbol.defaultValue(), value) && doFilter(key, value);
	}

	protected abstract boolean doFilter(Tuple key, T value);

	public abstract Object[] forwardMap(Tuple key, T value);

	@Override
	public Iterable<Object[]> getAll(Model model) {
		return (() -> new CursorAsIterator<>(model.getInterpretation(symbol).getAll(), this::forwardMap, this::filter));
	}

	@Override
	public String toString() {
		return name();
	}

	@Override
	public String toReferenceString() {
		return "@RelationView(\"%s\") %s".formatted(viewName, symbol.name());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SymbolView<?> that = (SymbolView<?>) o;
		return Objects.equals(symbol, that.symbol) && Objects.equals(viewName, that.viewName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), symbol, viewName);
	}
}
