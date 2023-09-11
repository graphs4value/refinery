/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.internal;

import tools.refinery.store.adapter.AdapterUtils;
import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.Version;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.model.ModelDiffCursor;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.util.CancellationToken;

import java.util.*;

public class ModelStoreImpl implements ModelStore {
	private final LinkedHashMap<? extends AnySymbol, ? extends VersionedMapStore<Tuple, ?>> stores;
	private final List<ModelStoreAdapter> adapters;
	private final CancellationToken cancellationToken;

	ModelStoreImpl(LinkedHashMap<? extends AnySymbol, ? extends VersionedMapStore<Tuple, ?>> stores, int adapterCount,
				   CancellationToken cancellationToken) {
		this.stores = stores;
		adapters = new ArrayList<>(adapterCount);
		this.cancellationToken = cancellationToken;
	}

	@Override
	public Collection<AnySymbol> getSymbols() {
		return Collections.unmodifiableCollection(stores.keySet());
	}

	private ModelImpl createModelWithoutInterpretations(Version state) {
		return new ModelImpl(this, state, adapters.size());
	}

	@Override
	public ModelImpl createEmptyModel() {
		var model = createModelWithoutInterpretations(null);
		var interpretations = new LinkedHashMap<AnySymbol, VersionedInterpretation<?>>(stores.size());
		for (var entry : this.stores.entrySet()) {
			var symbol = entry.getKey();
			interpretations.put(symbol, VersionedInterpretation.of(model, symbol, entry.getValue()));
		}
		model.setInterpretations(interpretations);
		adaptModel(model);
		return model;
	}

	@Override
	public synchronized ModelImpl createModelForState(Version state) {
		var model = createModelWithoutInterpretations(state);
		var interpretations = new LinkedHashMap<AnySymbol, VersionedInterpretation<?>>(stores.size());

		int i=0;
		for (var entry : this.stores.entrySet()) {
			var symbol = entry.getKey();
			interpretations.put(symbol,
					VersionedInterpretation.of(
							model,
							symbol,
							entry.getValue(),
							ModelVersion.getInternalVersion(state,i++)));
		}

		model.setInterpretations(interpretations);
		adaptModel(model);
		return model;
	}

	private void adaptModel(ModelImpl model) {
		for (var storeAdapter : adapters) {
			var adapter = storeAdapter.createModelAdapter(model);
			model.addAdapter(adapter);
		}
	}

	@Override
	public synchronized ModelDiffCursor getDiffCursor(Version from, Version to) {
		var diffCursors = new HashMap<AnySymbol, DiffCursor<?, ?>>();
		for (var entry : stores.entrySet()) {
			var representation = entry.getKey();
			var diffCursor = entry.getValue().getDiffCursor(from, to);
			diffCursors.put(representation, diffCursor);
		}
		return new ModelDiffCursor(diffCursors);
	}

	@Override
	public <T extends ModelStoreAdapter> Optional<T> tryGetAdapter(Class<? extends T> adapterType) {
		return AdapterUtils.tryGetAdapter(adapters, adapterType);
	}

	@Override
	public <T extends ModelStoreAdapter> T getAdapter(Class<T> adapterType) {
		return AdapterUtils.getAdapter(adapters, adapterType);
	}

	void addAdapter(ModelStoreAdapter adapter) {
		adapters.add(adapter);
	}

	@Override
	public void checkCancelled() {
		cancellationToken.checkCancelled();
	}

	CancellationToken getCancellationToken() {
		return cancellationToken;
	}
}
