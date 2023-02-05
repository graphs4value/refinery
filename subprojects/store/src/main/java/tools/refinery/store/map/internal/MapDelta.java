package tools.refinery.store.map.internal;

public record MapDelta<K, V>(K key, V oldValue, V newValue) {
	public K getKey() {
		return key;
	}

	public V getOldValue() {
		return oldValue;
	}

	public V getNewValue() {
		return newValue;
	}
}
