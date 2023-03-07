package tools.refinery.store.query.literal;

public interface CanNegate<T extends CanNegate<T>> extends Literal {
	T negate();
}
