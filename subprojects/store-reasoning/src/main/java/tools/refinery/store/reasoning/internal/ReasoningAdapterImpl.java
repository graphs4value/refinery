package tools.refinery.store.reasoning.internal;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.PartialInterpretation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.ResultSet;

public class ReasoningAdapterImpl implements ReasoningAdapter {
	private final Model model;
	private final ReasoningStoreAdapterImpl storeAdapter;

	ReasoningAdapterImpl(Model model, ReasoningStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public ReasoningStoreAdapterImpl getStoreAdapter() {
		return storeAdapter;
	}

	@Override
	public <A, C> PartialInterpretation<A, C> getPartialInterpretation(PartialSymbol<A, C> partialSymbol) {
		return null;
	}

	@Override
	public ResultSet getLiftedResultSet(Dnf query) {
		return null;
	}
}
