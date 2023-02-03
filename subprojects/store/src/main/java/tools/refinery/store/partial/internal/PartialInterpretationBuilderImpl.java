package tools.refinery.store.partial.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.partial.PartialInterpretationBuilder;

public class PartialInterpretationBuilderImpl extends AbstractModelAdapterBuilder implements PartialInterpretationBuilder {
	public PartialInterpretationBuilderImpl(ModelStoreBuilder storeBuilder) {
		super(storeBuilder);
	}

	@Override
	public PartialInterpretationStoreAdapterImpl createStoreAdapter(ModelStore store) {
		return null;
	}
}
