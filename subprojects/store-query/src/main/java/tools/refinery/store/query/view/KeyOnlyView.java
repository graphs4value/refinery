/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public final class KeyOnlyView<T> extends TuplePreservingView<T> {
	public static final String VIEW_NAME = "key";

	private final T defaultValue;

	public KeyOnlyView(Symbol<T> symbol) {
		super(symbol, VIEW_NAME);
		defaultValue = symbol.defaultValue();
	}

	@Override
	protected boolean doFilter(Tuple key, T value) {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		KeyOnlyView<?> that = (KeyOnlyView<?>) o;
		return Objects.equals(defaultValue, that.defaultValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), defaultValue);
	}
}
