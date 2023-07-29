/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.internal;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;

import java.util.Collection;

public class StateCoderStoreAdapterImpl implements StateCoderStoreAdapter {
	final ModelStore store;
	final Collection<Symbol<?>> symbols;

	StateCoderStoreAdapterImpl(ModelStore store, Collection<Symbol<?>> symbols) {
		this.store = store;
		this.symbols = symbols;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public StateCoderAdapter createModelAdapter(Model model) {
		return new StateCoderAdapterImpl(this,model,symbols);
	}
}
