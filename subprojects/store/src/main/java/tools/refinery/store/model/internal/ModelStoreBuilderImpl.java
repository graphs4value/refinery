/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.internal;

import tools.refinery.store.adapter.AdapterUtils;
import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreFactory;
import tools.refinery.store.map.VersionedMapStoreFactoryBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.util.CancellationToken;

import java.util.*;

public class ModelStoreBuilderImpl implements ModelStoreBuilder {
	private CancellationToken cancellationToken;
	private final LinkedHashSet<AnySymbol> allSymbols = new LinkedHashSet<>();
	private final LinkedHashMap<SymbolEquivalenceClass<?>, List<AnySymbol>> equivalenceClasses = new LinkedHashMap<>();
	private final List<ModelAdapterBuilder> adapters = new ArrayList<>();

	@Override
	public ModelStoreBuilder cancellationToken(CancellationToken cancellationToken) {
		if (this.cancellationToken != null) {
			throw new IllegalStateException("Cancellation token was already set");
		}
		if (cancellationToken == null) {
			throw new IllegalStateException("Cancellation token must not be null");
		}
		this.cancellationToken = cancellationToken;
		return this;
	}

	@Override
	public <T> ModelStoreBuilder symbol(Symbol<T> symbol) {
		if (!allSymbols.add(symbol)) {
			// No need to add symbol twice.
			return this;
		}
		var equivalenceClass = new SymbolEquivalenceClass<>(symbol);
		var symbolsInEquivalenceClass = equivalenceClasses.computeIfAbsent(equivalenceClass,
				ignored -> new ArrayList<>());
		symbolsInEquivalenceClass.add(symbol);
		return this;
	}

	@Override
	public ModelStoreBuilder with(ModelAdapterBuilder adapterBuilder) {
		for (var existingAdapter : adapters) {
			if (existingAdapter.getClass().equals(adapterBuilder.getClass())) {
				throw new IllegalArgumentException("%s adapter was already configured for store builder"
						.formatted(adapterBuilder.getClass().getName()));
			}
		}
		adapters.add(adapterBuilder);
		return this;
	}

	@Override
	public ModelStoreBuilder with(ModelStoreConfiguration configuration) {
		configuration.apply(this);
		return this;
	}

	@Override
	public <T extends ModelAdapterBuilder> Optional<T> tryGetAdapter(Class<? extends T> adapterType) {
		return AdapterUtils.tryGetAdapter(adapters, adapterType);
	}

	@Override
	public <T extends ModelAdapterBuilder> T getAdapter(Class<T> adapterType) {
		return AdapterUtils.getAdapter(adapters, adapterType);
	}

	@Override
	public ModelStore build() {
		// First configure adapters and let them register any symbols we don't know about yet.
		for (int i = adapters.size() - 1; i >= 0; i--) {
			adapters.get(i).configure(this);
		}
		var stores = new LinkedHashMap<AnySymbol, VersionedMapStore<Tuple, ?>>(allSymbols.size());
		for (var entry : equivalenceClasses.entrySet()) {
			createStores(stores, entry.getKey(), entry.getValue());
		}
		var modelStore = new ModelStoreImpl(stores, adapters.size(), cancellationToken == null ?
				CancellationToken.NONE : cancellationToken);
		for (var adapterBuilder : adapters) {
			var storeAdapter = adapterBuilder.build(modelStore);
			modelStore.addAdapter(storeAdapter);
		}
		return modelStore;
	}

	private <T> void createStores(Map<AnySymbol, VersionedMapStore<Tuple, ?>> stores,
								  SymbolEquivalenceClass<T> equivalenceClass, List<AnySymbol> symbols) {
		int size = symbols.size();
		VersionedMapStoreFactory<Tuple, T> mapFactory = VersionedMapStore
				.<Tuple, T>builder()
				.strategy(VersionedMapStoreFactoryBuilder.StoreStrategy.DELTA)
				.defaultValue(equivalenceClass.defaultValue())
				.build();
		var storeGroup = mapFactory.createGroup(size);
		for (int i = 0; i < size; i++) {
			stores.put(symbols.get(i), storeGroup.get(i));
		}
	}
}
