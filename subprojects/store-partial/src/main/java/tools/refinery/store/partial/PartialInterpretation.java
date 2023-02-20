package tools.refinery.store.partial;

import tools.refinery.store.partial.internal.PartialInterpretationBuilderImpl;
import tools.refinery.store.adapter.ModelAdapterBuilderFactory;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.partial.representation.PartialRelation;

public final class PartialInterpretation extends ModelAdapterBuilderFactory<PartialInterpretationAdapter,
		PartialInterpretationStoreAdapter, PartialInterpretationBuilder> {
	public static final PartialInterpretation ADAPTER = new PartialInterpretation();

	public static final PartialRelation EXISTS = new PartialRelation("exists", 1);

	public static final PartialRelation EQUALS = new PartialRelation("equals", 1);

	private PartialInterpretation() {
		super(PartialInterpretationAdapter.class, PartialInterpretationStoreAdapter.class, PartialInterpretationBuilder.class);
	}

	@Override
	public PartialInterpretationBuilder createBuilder(ModelStoreBuilder storeBuilder) {
		return new PartialInterpretationBuilderImpl(storeBuilder);
	}
}
