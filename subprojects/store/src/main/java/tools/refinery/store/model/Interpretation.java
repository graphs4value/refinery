/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.Version;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

public non-sealed interface Interpretation<T> extends AnyInterpretation {
	@Override
	Symbol<T> getSymbol();

	T get(Tuple key);

	Cursor<Tuple, T> getAll();

	Cursor<Tuple, T> getAdjacent(int slot, int node);

	T put(Tuple key, T value);

	void putAll(Cursor<Tuple, T> cursor);

	DiffCursor<Tuple, T> getDiffCursor(Version to);

	void addListener(InterpretationListener<T> listener, boolean alsoWhenRestoring);

	void removeListener(InterpretationListener<T> listener);
}
