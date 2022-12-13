package tools.refinery.store.map;

import java.util.Set;

public interface Cursor<K, V> {
	K getKey();

	V getValue();

	boolean isTerminated();

	boolean move();

	default boolean isDirty() {
		return false;
	}

	default Set<AnyVersionedMap> getDependingMaps() {
		return Set.of();
	}
}
