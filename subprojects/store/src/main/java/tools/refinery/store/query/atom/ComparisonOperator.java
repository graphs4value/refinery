package tools.refinery.store.query.atom;

public enum ComparisonOperator {
	EQUALS,
	NOT_EQUALS,
	LESS,
	LESS_EQUALS,
	GREATER,
	GREATER_EQUALS;

	@Override
	public String toString() {
		return switch (this) {
			case EQUALS -> "==";
			case NOT_EQUALS -> "!=";
			case LESS -> "<";
			case LESS_EQUALS -> "<=";
			case GREATER -> ">";
			case GREATER_EQUALS -> ">=";
		};
	}
}
