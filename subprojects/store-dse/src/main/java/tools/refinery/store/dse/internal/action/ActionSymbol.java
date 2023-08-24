package tools.refinery.store.dse.internal.action;

import tools.refinery.store.tuple.Tuple;

public abstract class ActionSymbol implements AtomicAction {
	public abstract Tuple getValue(Tuple activation);
}
