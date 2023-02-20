package tools.refinery.store.partial;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.partial.representation.AnyPartialSymbol;
import tools.refinery.store.query.Dnf;

import java.util.Collection;

public interface PartialInterpretationStoreAdapter extends ModelStoreAdapter {
	Collection<AnyPartialSymbol> getPartialSymbols();

	Collection<Dnf> getLiftedQueries();

	@Override
	PartialInterpretationAdapter createModelAdapter(Model model);
}
