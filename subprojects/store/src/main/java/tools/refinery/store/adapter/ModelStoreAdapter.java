package tools.refinery.store.adapter;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;

public interface ModelStoreAdapter {
	ModelStore getStore();

	ModelAdapter createModelAdapter(Model model);
}
