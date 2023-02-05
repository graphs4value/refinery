package tools.refinery.store.map.internal;

import java.util.HashMap;
import java.util.Map;
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
		this.uncommittedOldValues.putIfAbsent(key, oldValue);
	}

	@Override
	public MapDelta<K, V>[] extractDeltas() {
		if (uncommittedOldValues.isEmpty()) {
			return null;
		} else {
			@SuppressWarnings("unchecked")
			MapDelta<K, V>[] deltas = new MapDelta[uncommittedOldValues.size()];
			int i = 0;
			for (Entry<K, V> entry : uncommittedOldValues.entrySet()) {
				final K key = entry.getKey();
				final V oldValue = entry.getValue();
				final V newValue = source.get(key);
				deltas[i] = new MapDelta<>(key, oldValue, newValue);
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
