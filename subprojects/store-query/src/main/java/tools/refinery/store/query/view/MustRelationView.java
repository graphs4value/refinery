package tools.refinery.store.query.view;

import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

public class MustRelationView extends TuplePreservingRelationView<TruthValue> {
	public MustRelationView(Symbol<TruthValue> symbol) {
		super(symbol, "must");
	}

	@Override
	public boolean filter(Tuple key, TruthValue value) {
		return value.must();
	}
}
