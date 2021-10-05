package tools.refinery.store.model;

import java.util.Map;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.model.representation.DataRepresentation;

public class ModelDiffCursor {
	final Map<DataRepresentation<?, ?>,DiffCursor<?,?>> diffcursors;

	public ModelDiffCursor(Map<DataRepresentation<?, ?>, DiffCursor<?, ?>> diffcursors) {
		super();
		this.diffcursors = diffcursors;
	}
	
	@SuppressWarnings("unchecked")
	public <K,V> DiffCursor<K,V> getCursor(DataRepresentation<K, V> representation) {
		Cursor<?, ?> cursor = diffcursors.get(representation);
		if(cursor != null) {
			return (DiffCursor<K, V>) cursor;
		} else {
			throw new IllegalArgumentException("ModelCursor does not contain cursor for representation "+representation);
		}
	}
}
