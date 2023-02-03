package tools.refinery.store.partial;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;

public interface PartialInterpretationBuilder extends ModelAdapterBuilder {
	@Override
	PartialInterpretationStoreAdapter createStoreAdapter(ModelStore store);
}
