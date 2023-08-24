package tools.refinery.store.dse.internal.action;

import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

public class ActivationSymbol extends ActionSymbol {

	private final int index;
	private Tuple value = null;

	public ActivationSymbol() {
		this(0);
	}

	public ActivationSymbol(int index) {
		this.index = index;
	}

	@Override
	public void fire(Tuple activation) {
		value = Tuple.of(activation.get(index));
	}

	@Override
	public ActivationSymbol prepare(Model model) {
		return this;
	}

	@Override
	public Tuple getValue(Tuple activation) {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ActivationSymbol)) {
			return false;
		}
        return index == ((ActivationSymbol) obj).index;
    }

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + Integer.hashCode(index);
		return result;
	}
}
