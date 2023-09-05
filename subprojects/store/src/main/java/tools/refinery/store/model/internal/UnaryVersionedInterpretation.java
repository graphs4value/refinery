/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.internal;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

class UnaryVersionedInterpretation<T> extends VersionedInterpretation<T> {
	public UnaryVersionedInterpretation(ModelImpl model, Symbol<T> symbol, VersionedMap<Tuple, T> map) {
		super(model, symbol, map);
	}

	private void validateSlot(int slot) {
		if (slot != 0) {
			throw new IllegalArgumentException("Invalid index: " + slot);
		}
	}

	@Override
	public Cursor<Tuple, T> getAdjacent(int slot, int node) {
		validateSlot(slot);
		var key = Tuple.of(node);
		var value = get(key);
		if (Objects.equals(value, getSymbol().defaultValue())) {
			return Cursors.empty();
		}
		return Cursors.singleton(key, value);
	}

	@Override
	public int getAdjacentSize(int slot, int node) {
		validateSlot(slot);
		var key = Tuple.of(node);
		var value = get(key);
		if (Objects.equals(value, getSymbol().defaultValue())) {
			return 0;
		}
		return 1;
	}
}
