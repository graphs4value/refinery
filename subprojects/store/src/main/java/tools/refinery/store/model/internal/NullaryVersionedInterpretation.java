/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.internal;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

class NullaryVersionedInterpretation<T> extends VersionedInterpretation<T> {
	public NullaryVersionedInterpretation(ModelImpl model, Symbol<T> symbol, VersionedMap<Tuple, T> map) {
		super(model, symbol, map);
	}

	@Override
	public Cursor<Tuple, T> getAdjacent(int slot, int node) {
		throw new IllegalArgumentException("Invalid index: " + slot);
	}

	@Override
	public int getAdjacentSize(int slot, int node) {
		throw new IllegalArgumentException("Invalid index: " + slot);
	}
}
