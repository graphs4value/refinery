package tools.refinery.store.dse.internal.action;

import tools.refinery.store.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

public class NewItemSymbol extends ActionSymbol {
	private DesignSpaceExplorationAdapter dseAdapter;
	private Tuple1 value;

	@Override
	public void fire(Tuple activation) {
		value = dseAdapter.createObject();
	}

	@Override
	public NewItemSymbol prepare(Model model) {
		dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		return this;
	}

	@Override
	public Tuple1 getValue(Tuple activation) {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
        return obj instanceof NewItemSymbol;
    }

	@Override
	public int hashCode() {
		return 42;
	}
}
