package tools.refinery.store.map;

public non-sealed interface VersionedMap<K, V> extends AnyVersionedMap {
	V get(K key);

	Cursor<K, V> getAll();

	V put(K key, V value);

	void putAll(Cursor<K, V> cursor);

	DiffCursor<K, V> getDiffCursor(long state);
}
