package tools.refinery.visualization.internal;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.visualization.ModelVisualizerStoreAdapter;

public class ModelVisualizeStoreAdapterImpl implements ModelVisualizerStoreAdapter {
	private final ModelStore store;

	public ModelVisualizeStoreAdapterImpl(ModelStore store) {
		this.store = store;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public ModelAdapter createModelAdapter(Model model) {
		return new ModelVisualizerAdapterImpl(model, this);
	}
}
