/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.internal;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import tools.refinery.store.map.*;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

class BaseIndexer<T> {
	private final MutableIntObjectMap<MutableMap<Tuple, T>>[] maps;
	private final VersionedMap<Tuple, T> versionedMap;

	public BaseIndexer(int arity, VersionedMap<Tuple, T> map) {
		if (arity < 2) {
			throw new IllegalArgumentException("Only arity >= 2 symbols need to be indexed");
		}
		// There is no way in Java to create a generic array in a checked way.
		@SuppressWarnings({"unchecked", "squid:S1905"})
		var uncheckedMaps = (MutableIntObjectMap<MutableMap<Tuple, T>>[]) new MutableIntObjectMap[arity];
		maps = uncheckedMaps;
		for (int i = 0; i < arity; i++) {
			maps[i] = IntObjectMaps.mutable.empty();
		}
		this.versionedMap = map;
		if (map != null) {
			var cursor = map.getAll();
			while (cursor.move()) {
				put(cursor.getKey(), cursor.getValue());
			}
		}
	}

	public void put(Tuple key, T value) {
		for (int i = 0; i < maps.length; i++) {
			var map = maps[i];
			int element = key.get(i);
			var adjacentTuples = map.getIfAbsentPut(element, Maps.mutable::empty);
			adjacentTuples.put(key, value);
		}
	}

	public void remove(Tuple key) {
		for (int i = 0; i < maps.length; i++) {
			var map = maps[i];
			int element = key.get(i);
			var adjacentTuples = map.get(element);
			if (adjacentTuples == null) {
				continue;
			}
			adjacentTuples.remove(key);
			if (adjacentTuples.isEmpty()) {
				map.remove(element);
			}
		}
	}

	private MutableMap<Tuple, T> getAdjacentMap(int slot, int node) {
		if (slot < 0 || slot >= maps.length) {
			throw new IllegalArgumentException("Invalid index: " + slot);
		}
		var map = maps[slot];
		return map.get(node);
	}

	public int getAdjacentSize(int slot, int node) {
		var adjacentTuples = getAdjacentMap(slot, node);
		if (adjacentTuples == null) {
			return 0;
		}
		return adjacentTuples.size();
	}

	public Cursor<Tuple, T> getAdjacent(int slot, int node) {
		var adjacentTuples = getAdjacentMap(slot, node);
		if (adjacentTuples == null) {
			return Cursors.empty();
		}
		return new IndexCursor<>(adjacentTuples, versionedMap);
	}

	private static class IndexCursor<T> extends IteratorBasedCursor<Tuple, T> {
		private final Set<AnyVersionedMap> dependingMaps;

		public IndexCursor(MutableMap<Tuple, T> map, VersionedMap<Tuple, T> versionedMap) {
			super(map.entrySet().iterator());
			dependingMaps = versionedMap == null ? Set.of() : Set.of(versionedMap);
		}

		@Override
		public Set<AnyVersionedMap> getDependingMaps() {
			return dependingMaps;
		}
	}
}
