/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.delta;

import java.util.ArrayList;
import java.util.List;

public class UncommittedDeltaArrayStore<K, V> implements UncommittedDeltaStore<K, V> {
	final List<MapDelta<K, V>> uncommittedOldValues = new ArrayList<>();

	@Override
	public void processChange(K key, V oldValue, V newValue) {
		uncommittedOldValues.add(new MapDelta<>(key, oldValue, newValue));
	}

	@Override
	public MapDelta<K, V>[] extractDeltas() {
		if (uncommittedOldValues.isEmpty()) {
			return null;
		} else {
			@SuppressWarnings("unchecked")
			MapDelta<K, V>[] result = uncommittedOldValues.toArray(new MapDelta[0]);
			return result;
		}
	}

	@Override
	public MapDelta<K, V>[] extractAndDeleteDeltas() {
		MapDelta<K, V>[] res = extractDeltas();
		this.uncommittedOldValues.clear();
		return res;
	}
}
