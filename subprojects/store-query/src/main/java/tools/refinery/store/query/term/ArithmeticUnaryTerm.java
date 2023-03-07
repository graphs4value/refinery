package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;

import java.util.Objects;

public abstract class ArithmeticUnaryTerm<T> extends UnaryTerm<T, T> {
	private final ArithmeticUnaryOperator operator;

	protected ArithmeticUnaryTerm(ArithmeticUnaryOperator operator, Term<T> body) {
		super(body);
		this.operator = operator;
	}

	@Override
	public Class<T> getBodyType() {
		return getType();
	}

	public ArithmeticUnaryOperator getOperator() {
		return operator;
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherArithmeticUnaryTerm = (ArithmeticUnaryTerm<?>) other;
		return operator == otherArithmeticUnaryTerm.operator;
	}

	@Override
	public String toString() {
		return operator.formatString(getBody().toString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		ArithmeticUnaryTerm<?> that = (ArithmeticUnaryTerm<?>) o;
		return operator == that.operator;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), operator);
	}
}
