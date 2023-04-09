/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query;

import tools.refinery.store.adapter.ModelAdapterType;

public final class ModelQuery extends ModelAdapterType<ModelQueryAdapter, ModelQueryStoreAdapter, ModelQueryBuilder> {
	public static final ModelQuery ADAPTER = new ModelQuery();

	private ModelQuery() {
		super(ModelQueryAdapter.class, ModelQueryStoreAdapter.class, ModelQueryBuilder.class);
	}
}
