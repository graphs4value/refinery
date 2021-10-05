package tools.refinery.store.model;

import java.util.Map;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.representation.DataRepresentation;

public class ModelCursor {
	final Map<DataRepresentation<?, ?>,Cursor<?,?>> cursors;

	public ModelCursor(Map<DataRepresentation<?, ?>, Cursor<?, ?>> cursors) {
		super();
		this.cursors = cursors;
	}
	
	@SuppressWarnings("unchecked")
	public <K,V> Cursor<K,V> getCursor(DataRepresentation<K, V> representation) {
		Cursor<?, ?> cursor = cursors.get(representation);
		if(cursor != null) {
			return (Cursor<K, V>) cursor;
		} else {
			throw new IllegalArgumentException("ModelCursor does not contain cursor for representation "+representation);
		}
	}
}
