/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope.internal;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.scope.ScopePropagatorStoreAdapter;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.cardinality.CardinalityInterval;

import java.util.List;
import java.util.Map;

// Not a record, because we want to control getter visibility.
@SuppressWarnings("ClassCanBeRecord")
class ScopePropagatorStoreAdapterImpl implements ScopePropagatorStoreAdapter {
	private final ModelStore store;
	private final Symbol<CardinalityInterval> countSymbol;
	private final Map<PartialRelation, CardinalityInterval> scopes;
	private final List<TypeScopePropagator.Factory> propagatorFactories;

	public ScopePropagatorStoreAdapterImpl(
			ModelStore store, Symbol<CardinalityInterval> countSymbol,
			Map<PartialRelation, CardinalityInterval> scopes, List<TypeScopePropagator.Factory> propagatorFactories) {
		this.store = store;
		this.countSymbol = countSymbol;
		this.scopes = scopes;
		this.propagatorFactories = propagatorFactories;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	Symbol<CardinalityInterval> getCountSymbol() {
		return countSymbol;
	}

	@Override
	public Map<PartialRelation, CardinalityInterval> getScopes() {
		return scopes;
	}

	public List<TypeScopePropagator.Factory> getPropagatorFactories() {
		return propagatorFactories;
	}

	@Override
	public ModelAdapter createModelAdapter(Model model) {
		return new ScopePropagatorAdapterImpl(model, this);
	}
}
