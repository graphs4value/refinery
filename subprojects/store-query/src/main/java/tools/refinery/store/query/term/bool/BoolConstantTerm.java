package tools.refinery.store.query.term.bool;

import tools.refinery.store.query.term.ConstantTerm;

public final class BoolConstantTerm {
	public static final ConstantTerm<Boolean> TRUE = new ConstantTerm<>(Boolean.class, true);
	public static final ConstantTerm<Boolean> FALSE = new ConstantTerm<>(Boolean.class, false);

	private BoolConstantTerm() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static ConstantTerm<Boolean> valueOf(boolean boolValue) {
		return boolValue ? TRUE : FALSE;
	}
}
