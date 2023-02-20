package tools.refinery.store.partial;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.partial.representation.AnyPartialSymbol;
import tools.refinery.store.partial.representation.PartialSymbol;
import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.ResultSet;

public interface PartialInterpretationAdapter extends ModelAdapter {
	@Override
	PartialInterpretationStoreAdapter getStoreAdapter();

	default AnyPartialSymbolInterpretation getPartialInterpretation(AnyPartialSymbol partialSymbol) {
		return getPartialInterpretation((PartialSymbol<?, ?>) partialSymbol);
	}

	<A, C> PartialSymbolInterpretation<A, C> getPartialInterpretation(PartialSymbol<A, C> partialSymbol);

	ResultSet getLiftedResultSet(Dnf query);
}
