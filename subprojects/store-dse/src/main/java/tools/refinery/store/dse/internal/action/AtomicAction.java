package tools.refinery.store.dse.internal.action;

import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

public interface AtomicAction {

	void fire(Tuple activation);

	AtomicAction prepare(Model model);

	boolean equalsWithSubstitution(AtomicAction other);
}
