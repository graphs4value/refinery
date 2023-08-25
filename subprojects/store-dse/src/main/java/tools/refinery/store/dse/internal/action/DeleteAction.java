package tools.refinery.store.dse.internal.action;

import tools.refinery.store.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

public class DeleteAction implements AtomicAction {

	private final ActionSymbol symbol;
	private DesignSpaceExplorationAdapter dseAdapter;

	public DeleteAction(ActionSymbol symbol) {
		this.symbol = symbol;
	}

	@Override
	public void fire(Tuple activation) {
		dseAdapter.deleteObject(symbol.getValue(activation));
	}

	@Override
	public DeleteAction prepare(Model model) {
		dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof DeleteAction other)) {
			return false;
		}
		return this.symbol.getClass() == other.symbol.getClass();
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + symbol.getClass().hashCode();
		return result;
	}
}
