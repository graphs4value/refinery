package tools.refinery.store.dse.internal.action;

import tools.refinery.store.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

public class NewItemVariable implements ActionVariable {
	private DesignSpaceExplorationAdapter dseAdapter;
	private Tuple1 value;

	@Override
	public void fire(Tuple activation) {
		value = dseAdapter.createObject();
	}

	@Override
	public NewItemVariable prepare(Model model) {
		dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		return this;
	}

	@Override
	public Tuple1 getValue() {
		return value;
	}

	@Override
	public boolean equalsWithSubstitution(AtomicAction other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherAction = (NewItemVariable) other;
		if (value == null) {
			return otherAction.value == null;
		}
		return value.equals(otherAction.value);

	}
}
