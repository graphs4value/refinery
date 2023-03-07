package tools.refinery.store.query.term;

public enum ArithmeticUnaryOperator {
	PLUS("+"),
	MINUS("-");

	private final String prefix;

	ArithmeticUnaryOperator(String prefix) {
		this.prefix = prefix;
	}

	public String formatString(String body) {
		return "%s(%s)".formatted(prefix, body);
	}
}
