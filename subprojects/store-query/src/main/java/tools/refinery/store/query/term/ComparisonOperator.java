package tools.refinery.store.query.term;

public enum ComparisonOperator {
	EQ("=="),
	NOT_EQ("!="),
	LESS("<"),
	LESS_EQ("<="),
	GREATER(">"),
	GREATER_EQ(">=");

	private final String text;

	ComparisonOperator(String text) {
		this.text = text;
	}

	public String formatString(String left, String right) {
		return "(%s) %s (%s)".formatted(left, text, right);
	}
}
