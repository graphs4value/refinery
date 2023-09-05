/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.modification.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.modification.ModificationBuilder;
import tools.refinery.store.dse.modification.ModificationStoreAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.statecoding.StateCoderBuilder;

public class ModificationBuilderImpl extends AbstractModelAdapterBuilder<ModificationStoreAdapter> implements ModificationBuilder {

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		storeBuilder.symbols(ModificationAdapterImpl.NEXT_ID);
		storeBuilder.tryGetAdapter(StateCoderBuilder.class).ifPresent(
				coderBuilder -> coderBuilder.exclude(ModificationAdapterImpl.NEXT_ID));
		super.doConfigure(storeBuilder);
	}

	@Override
	protected ModificationStoreAdapter doBuild(ModelStore store) {
		return new ModificationStoreAdapterImpl(store);
	}
}
