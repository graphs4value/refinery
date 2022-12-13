package tools.refinery.store.model;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.model.representation.AnyDataRepresentation;
import tools.refinery.store.model.representation.DataRepresentation;

import java.util.Map;

public class ModelDiffCursor {
	final Map<AnyDataRepresentation, DiffCursor<?, ?>> diffCursors;

	public ModelDiffCursor(Map<AnyDataRepresentation, DiffCursor<?, ?>> diffCursors) {
		super();
		this.diffCursors = diffCursors;
	}

	@SuppressWarnings("unchecked")
	public <K, V> DiffCursor<K, V> getCursor(DataRepresentation<K, V> representation) {
		Cursor<?, ?> cursor = diffCursors.get(representation);
		if (cursor != null) {
			return (DiffCursor<K, V>) cursor;
		} else {
			throw new IllegalArgumentException("ModelCursor does not contain cursor for representation " + representation);
		}
	}
}
