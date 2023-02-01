package tools.refinery.store.partial;

import tools.refinery.store.adapter.ModelAdapterBuilderFactory;
import tools.refinery.store.model.ModelStoreBuilder;

public final class PartialInterpretation extends ModelAdapterBuilderFactory<PartialModelAdapter,
		PartialModelStoreAdapter, PartialModelAdapterBuilder> {
	public static final PartialInterpretation ADAPTER = new PartialInterpretation();

	private PartialInterpretation() {
		super(PartialModelAdapter.class, PartialModelStoreAdapter.class, PartialModelAdapterBuilder.class);
	}

	@Override
	public PartialModelAdapterBuilder createBuilder(ModelStoreBuilder storeBuilder) {
		return new PartialModelAdapterBuilder(storeBuilder);
	}
}
