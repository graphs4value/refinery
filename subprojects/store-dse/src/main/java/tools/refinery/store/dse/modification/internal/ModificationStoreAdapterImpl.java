/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.modification.internal;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.modification.ModificationStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;

public class ModificationStoreAdapterImpl implements ModificationStoreAdapter {
	ModelStore store;

	ModificationStoreAdapterImpl(ModelStore store) {
		this.store = store;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public ModelAdapter createModelAdapter(Model model) {
		return new ModificationAdapterImpl(this, model);
	}
}
