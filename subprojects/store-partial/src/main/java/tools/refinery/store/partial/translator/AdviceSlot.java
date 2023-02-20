package tools.refinery.store.partial.translator;

import tools.refinery.store.representation.TruthValue;

public enum AdviceSlot {
	EXTEND_MUST(true),

	RESTRICT_MAY(true),

	/**
	 * Same as {@link #RESTRICT_MAY}, but only active if the value of the relation is not {@link TruthValue#TRUE} or
	 * {@link TruthValue#ERROR}.
	 */
	RESTRICT_NEW(false);

	private final boolean monotonic;

	AdviceSlot(boolean monotonic) {
		this.monotonic = monotonic;
	}

	public boolean isMonotonic() {
		return monotonic;
	}
}
