/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.internal;

import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderBuilder;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class StateCoderBuilderImpl implements StateCoderBuilder {
	Set<AnySymbol> excluded = new HashSet<>();

	@Override
	public StateCoderBuilder exclude(AnySymbol symbol) {
		excluded.add(symbol);
		return this;
	}

	@Override
	public boolean isConfigured() {
		return true;
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		// It does not modify the build process
	}

	@Override
	public StateCoderStoreAdapter build(ModelStore store) {
		Set<Symbol<?>> symbols = new LinkedHashSet<>();
		for (AnySymbol symbol : store.getSymbols()) {
			if (!excluded.contains(symbol) && (symbol instanceof Symbol<?> typed)) {
				symbols.add(typed);
			}
		}
		return new StateCoderStoreAdapterImpl(store, symbols);
	}
}
