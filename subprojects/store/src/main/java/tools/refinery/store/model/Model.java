package tools.refinery.store.model;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Versioned;
import tools.refinery.store.model.representation.AnyDataRepresentation;
import tools.refinery.store.model.representation.DataRepresentation;

import java.util.Set;

public interface Model extends Versioned {
	Set<AnyDataRepresentation> getDataRepresentations();

	<K, V> V get(DataRepresentation<K, V> representation, K key);

	<K, V> Cursor<K, V> getAll(DataRepresentation<K, V> representation);

	<K, V> V put(DataRepresentation<K, V> representation, K key, V value);

	<K, V> void putAll(DataRepresentation<K, V> representation, Cursor<K, V> cursor);

	long getSize(AnyDataRepresentation representation);

	ModelDiffCursor getDiffCursor(long to);
}
