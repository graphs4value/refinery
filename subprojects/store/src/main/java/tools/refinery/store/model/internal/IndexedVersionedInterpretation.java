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

import java.util.Objects;

class IndexedVersionedInterpretation<T> extends VersionedInterpretation<T> {
	private final BaseIndexer<T> indexer;

	public IndexedVersionedInterpretation(ModelImpl model, Symbol<T> symbol, VersionedMap<Tuple, T> map) {
		super(model, symbol, map);
		indexer = new BaseIndexer<>(symbol.arity(), map);
	}

	@Override
	public Cursor<Tuple, T> getAdjacent(int slot, int node) {
		return indexer.getAdjacent(slot, node);
	}

	@Override
	public int getAdjacentSize(int slot, int node) {
		return indexer.getAdjacentSize(slot, node);
	}

	@Override
	protected boolean shouldNotifyRestoreListeners() {
		// Always call the {@code valueChanged} method to update the index.
		return true;
	}

	@Override
	protected void valueChanged(Tuple key, T fromValue, T toValue, boolean restoring) {
		if (Objects.equals(toValue, getSymbol().defaultValue())) {
			indexer.remove(key);
		} else {
			indexer.put(key, toValue);
		}
		super.valueChanged(key, fromValue, toValue, restoring);
	}
}
