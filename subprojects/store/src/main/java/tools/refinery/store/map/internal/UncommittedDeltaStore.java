package tools.refinery.store.map.internal;

public interface UncommittedDeltaStore<K, V> {
	void processChange(K key, V oldValue, V newValue);

	MapDelta<K, V>[] extractDeltas();

	MapDelta<K, V>[] extractAndDeleteDeltas();

	default void checkIntegrity() {
		MapDelta<K, V>[] extractedDeltas = extractDeltas();
		if(extractedDeltas != null) {
			for(var uncommittedOldValue : extractedDeltas) {
				if(uncommittedOldValue == null) {
					throw new IllegalArgumentException("Null entry in deltas!");
				}
				if(uncommittedOldValue.getKey() == null) {
					throw new IllegalStateException("Null key in deltas!");
				}
			}
		}
	}

}
