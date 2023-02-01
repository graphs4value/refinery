package tools.refinery.store.partial;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.model.Model;

public class PartialModelAdapter implements ModelAdapter {
	private final Model model;
	private final PartialModelStoreAdapter storeAdapter;

	PartialModelAdapter(Model model, PartialModelStoreAdapter storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public PartialModelStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}
}
