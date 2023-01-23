package tools.refinery.store.adapter;

import tools.refinery.store.model.Model;

public interface ModelAdapter {
	Model getModel();

	ModelStoreAdapter getStoreAdapter();
}
