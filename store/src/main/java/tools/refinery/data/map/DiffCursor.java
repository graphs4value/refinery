package tools.refinery.data.map;

public interface DiffCursor<K, V> extends Cursor<K,V> {
	public V getFromValue();
	public V getToValue();
}