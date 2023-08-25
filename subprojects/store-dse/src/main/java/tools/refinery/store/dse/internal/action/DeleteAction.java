package tools.refinery.store.dse.internal.action;

import tools.refinery.store.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

public class DeleteAction implements AtomicAction {

	private final ActionVariable variable;
	private DesignSpaceExplorationAdapter dseAdapter;

	public DeleteAction(ActionVariable variable) {
		this.variable = variable;
	}

	@Override
	public void fire(Tuple activation) {
		dseAdapter.deleteObject(variable.getValue());
	}

	@Override
	public DeleteAction prepare(Model model) {
		dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		return this;
	}

	@Override
	public boolean equalsWithSubstitution(AtomicAction other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherAction = (DeleteAction) other;
		return this.variable.getClass() == otherAction.variable.getClass();
	}
}
