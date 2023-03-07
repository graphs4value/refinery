package tools.refinery.store.query.view;

import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

public class ForbiddenRelationView extends TuplePreservingRelationView<TruthValue> {
	public ForbiddenRelationView(Symbol<TruthValue> symbol) {
		super(symbol, "forbidden");
	}

	@Override
	public boolean filter(Tuple key, TruthValue value) {
		return !value.may();
	}
}
