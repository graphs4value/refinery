package tools.refinery.store.partial.internal;

import tools.refinery.store.model.Model;
import tools.refinery.store.partial.PartialInterpretationAdapter;
import tools.refinery.store.partial.PartialSymbolInterpretation;
import tools.refinery.store.partial.representation.PartialSymbol;
import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.ResultSet;

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

	@Override
	public <A, C> PartialSymbolInterpretation<A, C> getPartialInterpretation(PartialSymbol<A, C> partialSymbol) {
		return null;
	}

	@Override
	public ResultSet getLiftedResultSet(Dnf query) {
		return null;
	}
}
