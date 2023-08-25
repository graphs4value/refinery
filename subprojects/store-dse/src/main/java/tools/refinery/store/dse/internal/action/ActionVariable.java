package tools.refinery.store.dse.internal.action;

import tools.refinery.store.tuple.Tuple;

public interface ActionVariable extends AtomicAction {
	Tuple getValue();
}
