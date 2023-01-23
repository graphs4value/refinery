package tools.refinery.store.adapter;

import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;

public abstract class AbstractModelAdapterBuilder implements ModelAdapterBuilder {
	private final ModelStoreBuilder storeBuilder;

	protected AbstractModelAdapterBuilder(ModelStoreBuilder storeBuilder) {
		this.storeBuilder = storeBuilder;
	}

	@Override
	public <T extends ModelAdapterBuilder> T with(ModelAdapterBuilderFactory<?, ?, T> adapterBuilderFactory) {
		return storeBuilder.with(adapterBuilderFactory);
	}

	@Override
	public ModelStoreBuilder getStoreBuilder() {
		return storeBuilder;
	}

	@Override
	public ModelStore build() {
		return storeBuilder.build();
	}
}
