package tools.refinery.store.partial;

import tools.refinery.store.adapter.ModelAdapterBuilderFactory;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.partial.internal.PartialInterpretationBuilderImpl;

public final class PartialInterpretation extends ModelAdapterBuilderFactory<PartialInterpretationAdapter,
		PartialInterpretationStoreAdapter, PartialInterpretationBuilder> {
	public static final PartialInterpretation ADAPTER = new PartialInterpretation();

	private PartialInterpretation() {
		super(PartialInterpretationAdapter.class, PartialInterpretationStoreAdapter.class, PartialInterpretationBuilder.class);
	}

	@Override
	public PartialInterpretationBuilder createBuilder(ModelStoreBuilder storeBuilder) {
		return new PartialInterpretationBuilderImpl(storeBuilder);
	}
}
