package tools.refinery.store.partial.internal;

import tools.refinery.store.partial.PartialInterpretationStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.partial.representation.AnyPartialSymbol;
import tools.refinery.store.query.Dnf;

import java.util.Collection;

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
	public Collection<AnyPartialSymbol> getPartialSymbols() {
		return null;
	}

	@Override
	public Collection<Dnf> getLiftedQueries() {
		return null;
	}

	@Override
	public PartialInterpretationAdapterImpl createModelAdapter(Model model) {
		return new PartialInterpretationAdapterImpl(model, this);
	}
}
