/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.representation.Symbol;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class FilteredView<T> extends TuplePreservingView<T> {
	private final BiPredicate<Tuple, T> predicate;

	public FilteredView(Symbol<T> symbol, String name, BiPredicate<Tuple, T> predicate) {
		super(symbol, name);
		this.predicate = predicate;
	}

	public FilteredView(Symbol<T> symbol, BiPredicate<Tuple, T> predicate) {
		super(symbol);
		this.predicate = predicate;
	}

	public FilteredView(Symbol<T> symbol, String name, Predicate<T> predicate) {
		this(symbol, name, (k, v) -> predicate.test(v));
		validateDefaultValue(predicate);
	}

	public FilteredView(Symbol<T> symbol, Predicate<T> predicate) {
		this(symbol, (k, v) -> predicate.test(v));
		validateDefaultValue(predicate);
	}

	@Override
	protected boolean doFilter(Tuple key, T value) {
		return this.predicate.test(key, value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		FilteredView<?> that = (FilteredView<?>) o;
		return Objects.equals(predicate, that.predicate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), predicate);
	}

	private void validateDefaultValue(Predicate<T> predicate) {
		var defaultValue = getSymbol().defaultValue();
		boolean matchesDefaultValue = false;
		try {
			matchesDefaultValue = predicate.test(defaultValue);
		} catch (NullPointerException e) {
			if (defaultValue != null) {
				throw e;
			}
			// The predicate doesn't need to handle the default value if it is null.
		}
		if (matchesDefaultValue) {
			throw new InvalidQueryException("Tuples with default value %s cannot be enumerated in %s"
					.formatted(defaultValue, getSymbol()));
		}
	}
}
