package tools.refinery.store.map;

public interface DiffCursor<K, V> extends Cursor<K,V> {
	public V getFromValue();
	public V getToValue();
}