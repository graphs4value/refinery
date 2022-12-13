package tools.refinery.store.model.internal;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.internal.MapDiffCursor;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelDiffCursor;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.representation.AnyDataRepresentation;
import tools.refinery.store.model.representation.DataRepresentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModelImpl implements Model {
	private final ModelStore store;

	private final Map<AnyDataRepresentation, VersionedMap<?, ?>> maps;

	public ModelImpl(ModelStore store, Map<AnyDataRepresentation, VersionedMap<?, ?>> maps) {
		this.store = store;
		this.maps = maps;
	}

	@Override
	public Set<AnyDataRepresentation> getDataRepresentations() {
		return maps.keySet();
	}

	private VersionedMap<?, ?> getMap(AnyDataRepresentation representation) {
		if (maps.containsKey(representation)) {
			return maps.get(representation);
		} else {
			throw new IllegalArgumentException("Model does have representation " + representation);
		}
	}

	@SuppressWarnings("unchecked")
	private <K, V> VersionedMap<K, V> getMap(DataRepresentation<K, V> representation) {
		return (VersionedMap<K, V>) maps.get(representation);
	}

	private <K, V> VersionedMap<K, V> getMapValidateKey(DataRepresentation<K, V> representation, K key) {
		if (representation.isValidKey(key)) {
			return getMap(representation);
		} else {
			throw new IllegalArgumentException(
					"Key is not valid for representation! (representation=" + representation + ", key=" + key + ");");
		}
	}

	@Override
	public <K, V> V get(DataRepresentation<K, V> representation, K key) {
		return getMapValidateKey(representation, key).get(key);
	}

	@Override
	public <K, V> Cursor<K, V> getAll(DataRepresentation<K, V> representation) {
		return getMap(representation).getAll();
	}

	@Override
	public <K, V> V put(DataRepresentation<K, V> representation, K key, V value) {
		return getMapValidateKey(representation, key).put(key, value);
	}

	@Override
	public <K, V> void putAll(DataRepresentation<K, V> representation, Cursor<K, V> cursor) {
		getMap(representation).putAll(cursor);
	}

	@Override
	public long getSize(AnyDataRepresentation representation) {
		return getMap(representation).getSize();
	}

	@Override
	public ModelDiffCursor getDiffCursor(long to) {
		Model toModel = store.createModel(to);
		Map<AnyDataRepresentation, DiffCursor<?, ?>> diffCursors = new HashMap<>();
		for (AnyDataRepresentation anyDataRepresentation : this.maps.keySet()) {
			var dataRepresentation = (DataRepresentation<?, ?>) anyDataRepresentation;
			MapDiffCursor<?, ?> diffCursor = constructDiffCursor(toModel, dataRepresentation);
			diffCursors.put(dataRepresentation, diffCursor);
		}
		return new ModelDiffCursor(diffCursors);
	}

	private <K, V> MapDiffCursor<K, V> constructDiffCursor(Model toModel, DataRepresentation<K, V> representation) {
		@SuppressWarnings("unchecked")
		Cursor<K, V> fromCursor = (Cursor<K, V>) this.maps.get(representation).getAll();
		Cursor<K, V> toCursor = toModel.getAll(representation);

		ContinousHashProvider<K> hashProvider = representation.getHashProvider();
		V defaultValue = representation.getDefaultValue();
		return new MapDiffCursor<>(hashProvider, defaultValue, fromCursor, toCursor);
	}

	@Override
	public long commit() {
		long version = 0;
		boolean versionSet = false;
		for (VersionedMap<?, ?> map : maps.values()) {
			long newVersion = map.commit();
			if (versionSet) {
				if (version != newVersion) {
					throw new IllegalStateException(
							"Maps in model have different versions! (" + version + " and" + newVersion + ")");
				}
			} else {
				version = newVersion;
				versionSet = true;
			}
		}
		return version;
	}

	@Override
	public void restore(long state) {
		if (store.getStates().contains(state)) {
			for (VersionedMap<?, ?> map : maps.values()) {
				map.restore(state);
			}
		} else {
			throw new IllegalArgumentException("Map does not contain state " + state + "!");
		}
	}
}
