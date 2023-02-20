package tools.refinery.store.query.literal;

public final class Literals {
	private Literals() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static <T extends PolarLiteral<T>> T not(PolarLiteral<T> literal) {
		return literal.negate();
	}
}
