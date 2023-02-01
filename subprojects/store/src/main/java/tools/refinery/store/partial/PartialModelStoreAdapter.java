package tools.refinery.store.partial;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;

public class PartialModelStoreAdapter implements ModelStoreAdapter {
	private final ModelStore store;

	PartialModelStoreAdapter(ModelStore store) {
		this.store = store;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public PartialModelAdapter createModelAdapter(Model model) {
		return new PartialModelAdapter(model, this);
	}
}
