/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.internal;

import tools.refinery.store.adapter.AdapterList;
import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.adapter.ModelAdapterBuilderFactory;
import tools.refinery.store.adapter.ModelAdapterType;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreImpl;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.TupleHashProvider;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.*;

public class ModelStoreBuilderImpl implements ModelStoreBuilder {
	private final Set<AnySymbol> allSymbols = new HashSet<>();
	private final Map<SymbolEquivalenceClass<?>, List<AnySymbol>> equivalenceClasses = new HashMap<>();
	private final AdapterList<ModelAdapterBuilder> adapters = new AdapterList<>();

	@Override
	public <T> ModelStoreBuilder symbol(Symbol<T> symbol) {
		if (!allSymbols.add(symbol)) {
			throw new IllegalArgumentException("Symbol %s already added".formatted(symbol));
		}
		var equivalenceClass = new SymbolEquivalenceClass<>(symbol);
		var symbolsInEquivalenceClass = equivalenceClasses.computeIfAbsent(equivalenceClass,
				ignored -> new ArrayList<>());
		symbolsInEquivalenceClass.add(symbol);
		return this;
	}

	@Override
	public <T extends ModelAdapterBuilder> T with(ModelAdapterBuilderFactory<?, ?, T> adapterBuilderFactory) {
		return adapters.<T>tryGet(adapterBuilderFactory, adapterBuilderFactory.getModelAdapterBuilderClass())
				.orElseGet(() -> addAdapter(adapterBuilderFactory));
	}

	private <T extends ModelAdapterBuilder> T addAdapter(ModelAdapterBuilderFactory<?, ?, T> adapterBuilderFactory) {
		for (var configuredAdapterType : adapters.getAdapterTypes()) {
			var intersection = new HashSet<>(adapterBuilderFactory.getSupportedAdapterTypes());
			intersection.retainAll(configuredAdapterType.getSupportedAdapterTypes());
			if (!intersection.isEmpty()) {
				if (configuredAdapterType.supports(adapterBuilderFactory)) {
					// Impossible to end up here from <code>#with</code>, because we should have returned
					// the existing adapter there instead of adding a new one.
					throw new IllegalArgumentException(
							"Cannot add %s, because it is already provided by configured adapter %s"
									.formatted(adapterBuilderFactory, configuredAdapterType));
				} else if (adapterBuilderFactory.supports(configuredAdapterType)) {
					throw new IllegalArgumentException(
							"Cannot add %s, because it provides already configured adapter %s"
									.formatted(adapterBuilderFactory, configuredAdapterType));
				} else {
					throw new IllegalArgumentException(
							"Cannot add %s, because configured adapter %s already provides %s"
									.formatted(adapterBuilderFactory, configuredAdapterType, intersection));
				}
			}
		}
		var newAdapter = adapterBuilderFactory.createBuilder(this);
		adapters.add(adapterBuilderFactory, newAdapter);
		return newAdapter;
	}

	@Override
	public <T extends ModelAdapterBuilder> Optional<T> tryGetAdapter(ModelAdapterType<?, ?, ? extends T> adapterType) {
		return adapters.tryGet(adapterType, adapterType.getModelAdapterBuilderClass());
	}

	@Override
	public <T extends ModelAdapterBuilder> T getAdapter(ModelAdapterType<?, ?, T> adapterType) {
		return adapters.get(adapterType, adapterType.getModelAdapterBuilderClass());
	}

	@Override
	public ModelStore build() {
		var stores = new HashMap<AnySymbol, VersionedMapStore<Tuple, ?>>(allSymbols.size());
		for (var entry : equivalenceClasses.entrySet()) {
			createStores(stores, entry.getKey(), entry.getValue());
		}
		var modelStore = new ModelStoreImpl(stores, adapters.size());
		for (int i = adapters.size() - 1; i >= 0; i--) {
			adapters.get(i).configure();
		}
		for (var entry : adapters.withAdapterTypes()) {
			var adapter = entry.adapter().createStoreAdapter(modelStore);
			modelStore.addAdapter(entry.adapterType(), adapter);
		}
		return modelStore;
	}

	private <T> void createStores(Map<AnySymbol, VersionedMapStore<Tuple, ?>> stores,
								  SymbolEquivalenceClass<T> equivalenceClass, List<AnySymbol> symbols) {
		int size = symbols.size();
		var storeGroup = VersionedMapStoreImpl.createSharedVersionedMapStores(size, TupleHashProvider.INSTANCE,
				equivalenceClass.defaultValue());
		for (int i = 0; i < size; i++) {
			stores.put(symbols.get(i), storeGroup.get(i));
		}
	}
}
