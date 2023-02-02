package tools.refinery.store.model.internal;

import tools.refinery.store.adapter.AdapterList;
import tools.refinery.store.adapter.AnyModelAdapterType;
import tools.refinery.store.adapter.ModelAdapterType;
import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelDiffCursor;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.*;

public class ModelStoreImpl implements ModelStore {
	private final Map<? extends AnySymbol, ? extends VersionedMapStore<Tuple, ?>> stores;
	private final AdapterList<ModelStoreAdapter> adapters;

	ModelStoreImpl(Map<? extends AnySymbol, ? extends VersionedMapStore<Tuple, ?>> stores, int adapterCount) {
		this.stores = stores;
		adapters = new AdapterList<>(adapterCount);
	}

	@Override
	public Collection<AnySymbol> getSymbols() {
		return Collections.unmodifiableCollection(stores.keySet());
	}

	private ModelImpl createModelWithoutInterpretations(long state) {
		return new ModelImpl(this, state, adapters.size());
	}

	@Override
	public ModelImpl createEmptyModel() {
		var model = createModelWithoutInterpretations(Model.NO_STATE_ID);
		var interpretations = new HashMap<AnySymbol, VersionedInterpretation<?>>(stores.size());
		for (var entry : this.stores.entrySet()) {
			var symbol = entry.getKey();
			interpretations.put(symbol, VersionedInterpretation.of(model, symbol, entry.getValue()));
		}
		model.setInterpretations(interpretations);
		adaptModel(model);
		return model;
	}

	@Override
	public synchronized ModelImpl createModelForState(long state) {
		var model = createModelWithoutInterpretations(state);
		var interpretations = new HashMap<AnySymbol, VersionedInterpretation<?>>(stores.size());
		for (var entry : this.stores.entrySet()) {
			var symbol = entry.getKey();
			interpretations.put(symbol, VersionedInterpretation.of(model, symbol, entry.getValue(), state));
		}
		model.setInterpretations(interpretations);
		adaptModel(model);
		return model;
	}

	private void adaptModel(ModelImpl model) {
		for (var entry : adapters.withAdapterTypes()) {
			var adapter = entry.adapter().createModelAdapter(model);
			model.addAdapter(entry.adapterType(), adapter);
		}
	}

	@Override
	public synchronized Set<Long> getStates() {
		var iterator = stores.values().iterator();
		if (iterator.hasNext()) {
			return Set.copyOf(iterator.next().getStates());
		}
		return Set.of(0L);
	}

	@Override
	public synchronized ModelDiffCursor getDiffCursor(long from, long to) {
		var diffCursors = new HashMap<AnySymbol, DiffCursor<?, ?>>();
		for (var entry : stores.entrySet()) {
			var representation = entry.getKey();
			var diffCursor = entry.getValue().getDiffCursor(from, to);
			diffCursors.put(representation, diffCursor);
		}
		return new ModelDiffCursor(diffCursors);
	}

	@Override
	public <T extends ModelStoreAdapter> Optional<T> tryGetAdapter(ModelAdapterType<?, ? extends T, ?> adapterType) {
		return adapters.tryGet(adapterType, adapterType.getModelStoreAdapterClass());
	}

	@Override
	public <T extends ModelStoreAdapter> T getAdapter(ModelAdapterType<?, T, ?> adapterType) {
		return adapters.get(adapterType, adapterType.getModelStoreAdapterClass());
	}

	void addAdapter(AnyModelAdapterType adapterType, ModelStoreAdapter adapter) {
		adapters.add(adapterType, adapter);
	}
}
