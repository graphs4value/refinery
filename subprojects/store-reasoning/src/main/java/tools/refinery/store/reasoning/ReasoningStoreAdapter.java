package tools.refinery.store.reasoning;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.query.Dnf;

import java.util.Collection;

public interface ReasoningStoreAdapter extends ModelStoreAdapter {
	Collection<AnyPartialSymbol> getPartialSymbols();

	Collection<Dnf> getLiftedQueries();

	@Override
	ReasoningAdapter createModelAdapter(Model model);
}
