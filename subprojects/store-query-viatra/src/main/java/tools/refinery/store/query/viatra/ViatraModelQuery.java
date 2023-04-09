/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra;

import tools.refinery.store.adapter.ModelAdapterBuilderFactory;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQuery;
import tools.refinery.store.query.viatra.internal.ViatraModelQueryBuilderImpl;

public final class ViatraModelQuery extends ModelAdapterBuilderFactory<ViatraModelQueryAdapter,
		ViatraModelQueryStoreAdapter, ViatraModelQueryBuilder> {
	public static final ViatraModelQuery ADAPTER = new ViatraModelQuery();

	private ViatraModelQuery() {
		super(ViatraModelQueryAdapter.class, ViatraModelQueryStoreAdapter.class, ViatraModelQueryBuilder.class);
		extendsAdapter(ModelQuery.ADAPTER);
	}

	@Override
	public ViatraModelQueryBuilder createBuilder(ModelStoreBuilder storeBuilder) {
		return new ViatraModelQueryBuilderImpl(storeBuilder);
	}
}
