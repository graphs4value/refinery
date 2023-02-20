package tools.refinery.store.partial.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.partial.PartialInterpretationBuilder;
import tools.refinery.store.partial.literal.Modality;
import tools.refinery.store.query.Dnf;

public class PartialInterpretationBuilderImpl extends AbstractModelAdapterBuilder implements PartialInterpretationBuilder {
	public PartialInterpretationBuilderImpl(ModelStoreBuilder storeBuilder) {
		super(storeBuilder);
	}

	@Override
	public PartialInterpretationBuilder liftedQuery(Dnf liftedQuery) {
		return null;
	}

	@Override
	public Dnf lift(Modality modality, Dnf query) {
		return null;
	}

	@Override
	public PartialInterpretationStoreAdapterImpl createStoreAdapter(ModelStore store) {
		return null;
	}
}
