package tools.refinery.store.query.literal;

public interface PolarLiteral<T extends PolarLiteral<T>> extends Literal {
	T negate();
}
