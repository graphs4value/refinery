/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.adapter;

import tools.refinery.store.model.ModelStoreBuilder;

public abstract class ModelAdapterBuilderFactory<T1 extends ModelAdapter, T2 extends ModelStoreAdapter,
		T3 extends ModelAdapterBuilder> extends ModelAdapterType<T1, T2, T3> {

	protected ModelAdapterBuilderFactory(Class<T1> modelAdapterClass, Class<T2> modelStoreAdapterClass,
										 Class<T3> modelAdapterBuilderClass) {
		super(modelAdapterClass, modelStoreAdapterClass, modelAdapterBuilderClass);
	}

	public abstract T3 createBuilder(ModelStoreBuilder storeBuilder);
}
