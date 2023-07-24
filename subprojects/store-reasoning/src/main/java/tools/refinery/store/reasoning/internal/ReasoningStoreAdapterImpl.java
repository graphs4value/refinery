/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.query.dnf.Dnf;

import java.util.Collection;

public class ReasoningStoreAdapterImpl implements ReasoningStoreAdapter {
	private final ModelStore store;

	ReasoningStoreAdapterImpl(ModelStore store) {
		this.store = store;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public Collection<AnyPartialSymbol> getPartialSymbols() {
		return null;
	}

	@Override
	public Collection<Dnf> getLiftedQueries() {
		return null;
	}

	@Override
	public ReasoningAdapterImpl createModelAdapter(Model model) {
		return new ReasoningAdapterImpl(model, this);
	}
}
