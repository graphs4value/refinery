package tools.refinery.store.partial.internal;

import tools.refinery.store.model.Model;
import tools.refinery.store.partial.PartialInterpretationAdapter;

public class PartialInterpretationAdapterImpl implements PartialInterpretationAdapter {
	private final Model model;
	private final PartialInterpretationStoreAdapterImpl storeAdapter;

	PartialInterpretationAdapterImpl(Model model, PartialInterpretationStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public PartialInterpretationStoreAdapterImpl getStoreAdapter() {
		return storeAdapter;
	}
}
