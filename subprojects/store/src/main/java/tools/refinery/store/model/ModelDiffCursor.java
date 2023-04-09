/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model;

import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Map;

public class ModelDiffCursor {
	private final Map<? extends AnySymbol, ? extends DiffCursor<?, ?>> diffCursors;

	public ModelDiffCursor(Map<? extends AnySymbol, ? extends DiffCursor<?, ?>> diffCursors) {
		super();
		this.diffCursors = diffCursors;
	}

	public <T> DiffCursor<Tuple, T> getCursor(Symbol<T> symbol) {
		var cursor = diffCursors.get(symbol);
		if (cursor == null) {
			throw new IllegalArgumentException("No cursor for symbol %s".formatted(symbol));
		}
		@SuppressWarnings("unchecked")
		var typedCursor = (DiffCursor<Tuple, T>) cursor;
		return typedCursor;
	}
}
