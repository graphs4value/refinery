package tools.refinery.store.dse.internal.action;

import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

public class ActivationVariable implements ActionVariable {

	private final int index;
	private Tuple value;

	public ActivationVariable() {
		this(0);
	}

	public ActivationVariable(int index) {
		this.index = index;
	}

	@Override
	public void fire(Tuple activation) {
		value = Tuple.of(activation.get(index));
	}

	@Override
	public ActivationVariable prepare(Model model) {
		return this;
	}

	@Override
	public Tuple getValue() {
		return value;
	}

	@Override
	public boolean equalsWithSubstitution(AtomicAction other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherAction = (ActivationVariable) other;

		if (index != otherAction.index) {
			return false;
		}
		if (value == null) {
			return otherAction.value == null;
		}
		return value.equals(otherAction.value);
	}
}
