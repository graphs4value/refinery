package tools.refinery.store.query;

import tools.refinery.store.adapter.ModelAdapterType;

public final class ModelQuery extends ModelAdapterType<ModelQueryAdapter, ModelQueryStoreAdapter, ModelQueryBuilder> {
	public static final ModelQuery ADAPTER = new ModelQuery();

	private ModelQuery() {
		super(ModelQueryAdapter.class, ModelQueryStoreAdapter.class, ModelQueryBuilder.class);
	}
}
