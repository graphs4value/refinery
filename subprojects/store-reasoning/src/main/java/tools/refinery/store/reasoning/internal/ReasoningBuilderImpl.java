package tools.refinery.store.reasoning.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.query.Dnf;

public class ReasoningBuilderImpl extends AbstractModelAdapterBuilder implements ReasoningBuilder {
	public ReasoningBuilderImpl(ModelStoreBuilder storeBuilder) {
		super(storeBuilder);
	}

	@Override
	public ReasoningBuilder liftedQuery(Dnf liftedQuery) {
		return null;
	}

	@Override
	public Dnf lift(Modality modality, Dnf query) {
		return null;
	}

	@Override
	public ReasoningStoreAdapterImpl createStoreAdapter(ModelStore store) {
		return null;
	}
}
