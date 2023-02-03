package tools.refinery.store.partial.internal;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.partial.PartialInterpretationStoreAdapter;

public class PartialInterpretationStoreAdapterImpl implements PartialInterpretationStoreAdapter {
	private final ModelStore store;

	PartialInterpretationStoreAdapterImpl(ModelStore store) {
		this.store = store;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public PartialInterpretationAdapterImpl createModelAdapter(Model model) {
		return new PartialInterpretationAdapterImpl(model, this);
	}
}
