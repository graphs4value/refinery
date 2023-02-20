package tools.refinery.store.query.literal;

public enum LiteralReduction {
	/**
	 * Signifies that a literal should be preserved in the clause.
	 */
	NOT_REDUCIBLE,

	/**
	 * Signifies that the literal may be omitted from the cause (if the model being queried is nonempty).
	 */
	ALWAYS_TRUE,

	/**
	 * Signifies that the clause with the literal may be omitted entirely.
	 */
	ALWAYS_FALSE;

	public LiteralReduction negate() {
		return switch (this) {
			case NOT_REDUCIBLE -> NOT_REDUCIBLE;
			case ALWAYS_TRUE -> ALWAYS_FALSE;
			case ALWAYS_FALSE -> ALWAYS_TRUE;
		};
	}
}
