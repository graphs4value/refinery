/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.delta;

import java.util.*;
import java.util.Map.Entry;

import tools.refinery.store.map.VersionedMap;

public class UncommittedDeltaMapStore<K, V> implements UncommittedDeltaStore<K, V> {
	final VersionedMap<K, V> source;
	final Map<K, V> uncommittedOldValues = new HashMap<>();

	public UncommittedDeltaMapStore(VersionedMap<K, V> source) {
		this.source = source;
	}

	@Override
	public void processChange(K key, V oldValue, V newValue) {
		if(!uncommittedOldValues.containsKey(key)) {
			this.uncommittedOldValues.put(key,oldValue);
		}
	}

	@Override
	public MapDelta<K, V>[] extractDeltas() {
		if (uncommittedOldValues.isEmpty()) {
			return null;
		} else {
			@SuppressWarnings("unchecked")
			MapDelta<K,V>[] deltas = new MapDelta[uncommittedOldValues.size()];
			int i = 0;
			for (Entry<K, V> entry : uncommittedOldValues.entrySet()) {
				final K key = entry.getKey();
				final V oldValue = entry.getValue();
				final V newValue = source.get(key);
				deltas[i++] = new MapDelta<>(key, oldValue, newValue);
			}

			return deltas;
		}
	}

	@Override
	public MapDelta<K, V>[] extractAndDeleteDeltas() {
		MapDelta<K, V>[] res = extractDeltas();
		this.uncommittedOldValues.clear();
		return res;
	}
}
