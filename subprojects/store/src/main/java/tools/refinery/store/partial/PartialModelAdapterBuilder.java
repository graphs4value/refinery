package tools.refinery.store.partial;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;

public class PartialModelAdapterBuilder extends AbstractModelAdapterBuilder {
	PartialModelAdapterBuilder(ModelStoreBuilder storeBuilder) {
		super(storeBuilder);
	}

	@Override
	public PartialModelStoreAdapter createStoreAdapter(ModelStore store) {
		return null;
	}
}
