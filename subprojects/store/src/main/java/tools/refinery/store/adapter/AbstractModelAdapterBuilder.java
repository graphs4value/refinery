package tools.refinery.store.adapter;
/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;

public abstract class AbstractModelAdapterBuilder<T extends ModelStoreAdapter> implements ModelAdapterBuilder {
	private boolean configured;

	@Override
	public boolean isConfigured() {
		return configured;
	}

	protected void checkConfigured() {
		if (!configured) {
			throw new IllegalStateException("Model adapter builder was not configured");
		}
	}

	protected void checkNotConfigured() {
		if (configured) {
			throw new IllegalStateException("Model adapter builder was already configured");
		}
	}

	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		// Nothing to configure by default.
	}

	@Override
	public final void configure(ModelStoreBuilder storeBuilder) {
		checkNotConfigured();
		doConfigure(storeBuilder);
		configured = true;
	}

	protected abstract T doBuild(ModelStore store);

	@Override
	public final T build(ModelStore store) {
		checkConfigured();
		return doBuild(store);
	}
}
