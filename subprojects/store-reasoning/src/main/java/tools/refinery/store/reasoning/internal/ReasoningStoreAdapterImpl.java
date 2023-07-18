/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.refinement.StorageRefiner;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class ReasoningStoreAdapterImpl implements ReasoningStoreAdapter {
	private final ModelStore store;
	private final Map<AnyPartialSymbol, PartialInterpretation.Factory<?, ?>> symbolInterpreters;
	private final Map<AnyPartialSymbol, PartialInterpretationRefiner.Factory<?, ?>> symbolRefiners;
	private final Map<AnySymbol, StorageRefiner.Factory<?>> representationRefiners;
	private final Object initialModelLock = new Object();
	private final int initialNodeCount;
	private List<PartialModelInitializer> initializers;
	private long initialCommitId = Model.NO_STATE_ID;

	ReasoningStoreAdapterImpl(ModelStore store,
							  int initialNodeCount,
							  Map<AnyPartialSymbol, PartialInterpretation.Factory<?, ?>> symbolInterpreters,
							  Map<AnyPartialSymbol, PartialInterpretationRefiner.Factory<?, ?>> symbolRefiners,
							  Map<AnySymbol, StorageRefiner.Factory<?>> representationRefiners,
							  List<PartialModelInitializer> initializers) {
		this.store = store;
		this.initialNodeCount = initialNodeCount;
		this.symbolInterpreters = symbolInterpreters;
		this.symbolRefiners = symbolRefiners;
		this.representationRefiners = representationRefiners;
		this.initializers = initializers;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public Collection<AnyPartialSymbol> getPartialSymbols() {
		return symbolInterpreters.keySet();
	}

	@Override
	public Collection<AnyPartialSymbol> getRefinablePartialSymbols() {
		return symbolRefiners.keySet();
	}

	// Use of wildcard return value only in internal method not exposed as API, so there is less chance of confusion.
	@SuppressWarnings("squid:S1452")
	Map<AnyPartialSymbol, PartialInterpretation.Factory<?, ?>> getSymbolInterpreters() {
		return symbolInterpreters;
	}

	// Use of wildcard return value only in internal method not exposed as API, so there is less chance of confusion.
	@SuppressWarnings("squid:S1452")
	Map<AnyPartialSymbol, PartialInterpretationRefiner.Factory<?, ?>> getSymbolRefiners() {
		return symbolRefiners;
	}

	StorageRefiner[] createRepresentationRefiners(Model model) {
		var refiners = new StorageRefiner[representationRefiners.size()];
		int i = 0;
		for (var entry : representationRefiners.entrySet()) {
			var symbol = entry.getKey();
			var factory = entry.getValue();
			refiners[i] = createRepresentationRefiner(factory, model, symbol);
		}
		return refiners;
	}

	private <T> StorageRefiner createRepresentationRefiner(
			StorageRefiner.Factory<T> factory, Model model, AnySymbol symbol) {
		// The builder only allows well-typed assignment of refiners to symbols.
		@SuppressWarnings("unchecked")
		var typedSymbol = (Symbol<T>) symbol;
		return factory.create(typedSymbol, model);
	}

	@Override
	public Model createInitialModel() {
		synchronized (initialModelLock) {
			if (initialCommitId == Model.NO_STATE_ID) {
				return doCreateInitialModel();
			}
			return store.createModelForState(initialCommitId);
		}
	}

	private Model doCreateInitialModel() {
		var model = store.createEmptyModel();
		model.getInterpretation(ReasoningAdapterImpl.NODE_COUNT_SYMBOL).put(Tuple.of(), initialNodeCount);
		for (var initializer : initializers) {
			initializer.initialize(model, initialNodeCount);
		}
		model.getAdapter(ModelQueryAdapter.class).flushChanges();
		initialCommitId = model.commit();
		initializers = null;
		return model;
	}

	@Override
	public ReasoningAdapterImpl createModelAdapter(Model model) {
		return new ReasoningAdapterImpl(model, this);
	}
}
