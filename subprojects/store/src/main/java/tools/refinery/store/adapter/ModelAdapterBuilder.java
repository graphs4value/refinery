package tools.refinery.store.adapter;

import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;

public interface ModelAdapterBuilder {
	ModelStoreAdapter createStoreAdapter(ModelStore store);

	<T extends ModelAdapterBuilder> T with(ModelAdapterBuilderFactory<?, ?, T> adapterBuilderFactory);

	ModelStoreBuilder getStoreBuilder();

	default void configure() {
	}

	ModelStore build();
}
