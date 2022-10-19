package tools.refinery.store.model;

import tools.refinery.store.map.*;
import tools.refinery.store.model.internal.ModelImpl;
import tools.refinery.store.model.internal.SimilarRelationEquivalenceClass;
import tools.refinery.store.model.representation.AuxiliaryData;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.tuple.Tuple;

import java.util.*;
import java.util.Map.Entry;

public class ModelStoreImpl implements ModelStore {

	private final Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> stores;

	public ModelStoreImpl(Set<DataRepresentation<?, ?>> dataRepresentations) {
		stores = initStores(dataRepresentations);
	}

	private Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> initStores(
			Set<DataRepresentation<?, ?>> dataRepresentations) {
		Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> result = new HashMap<>();

		Map<SimilarRelationEquivalenceClass, List<Relation<?>>> symbolRepresentationsPerHashPerArity = new HashMap<>();

		for (DataRepresentation<?, ?> dataRepresentation : dataRepresentations) {
			if (dataRepresentation instanceof Relation<?> symbolRepresentation) {
				addOrCreate(symbolRepresentationsPerHashPerArity,
						new SimilarRelationEquivalenceClass(symbolRepresentation), symbolRepresentation);
			} else if (dataRepresentation instanceof AuxiliaryData<?, ?>) {
				VersionedMapStoreImpl<?, ?> store = new VersionedMapStoreImpl<>(dataRepresentation.getHashProvider(),
						dataRepresentation.getDefaultValue());
				result.put(dataRepresentation, store);
			} else {
				throw new UnsupportedOperationException(
						"Model store does not have strategy to use " + dataRepresentation.getClass() + "!");
			}
		}
		for (List<Relation<?>> symbolGroup : symbolRepresentationsPerHashPerArity.values()) {
			initRepresentationGroup(result, symbolGroup);
		}

		return result;
	}

	private void initRepresentationGroup(Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> result,
			List<Relation<?>> symbolGroup) {
		final ContinousHashProvider<Tuple> hashProvider = symbolGroup.get(0).getHashProvider();
		final Object defaultValue = symbolGroup.get(0).getDefaultValue();

		List<VersionedMapStore<Tuple, Object>> maps = VersionedMapStoreImpl
				.createSharedVersionedMapStores(symbolGroup.size(), hashProvider, defaultValue);

		for (int i = 0; i < symbolGroup.size(); i++) {
			result.put(symbolGroup.get(i), maps.get(i));
		}
	}

	private static <K, V> void addOrCreate(Map<K, List<V>> map, K key, V value) {
		List<V> list;
		if (map.containsKey(key)) {
			list = map.get(key);
		} else {
			list = new LinkedList<>();
			map.put(key, list);
		}
		list.add(value);
	}

	@Override
	public Set<DataRepresentation<?, ?>> getDataRepresentations() {
		return this.stores.keySet();
	}

	@Override
	public ModelImpl createModel() {
		Map<DataRepresentation<?, ?>, VersionedMap<?, ?>> maps = new HashMap<>();
		for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : this.stores.entrySet()) {
			maps.put(entry.getKey(), entry.getValue().createMap());
		}
		return new ModelImpl(this, maps);
	}

	@Override
	public synchronized ModelImpl createModel(long state) {
		Map<DataRepresentation<?, ?>, VersionedMap<?, ?>> maps = new HashMap<>();
		for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : this.stores.entrySet()) {
			maps.put(entry.getKey(), entry.getValue().createMap(state));
		}
		return new ModelImpl(this, maps);
	}

	@Override
	public synchronized Set<Long> getStates() {
		var iterator = stores.values().iterator();
		if (iterator.hasNext()) {
			return Set.copyOf(iterator.next().getStates());
		}
		return Set.of(0l);
	}

	@Override
	public synchronized ModelDiffCursor getDiffCursor(long from, long to) {
		Map<DataRepresentation<?, ?>, DiffCursor<?, ?>> diffcursors = new HashMap<>();
		for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : stores.entrySet()) {
			DataRepresentation<?, ?> representation = entry.getKey();
			DiffCursor<?, ?> diffCursor = entry.getValue().getDiffCursor(from, to);
			diffcursors.put(representation, diffCursor);
		}
		return new ModelDiffCursor(diffcursors);
	}
}
