package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.valuation.Valuation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class BinaryTerm<R, T1, T2> implements Term<R> {
	private final Term<T1> left;
	private final Term<T2> right;

	protected BinaryTerm(Term<T1> left, Term<T2> right) {
		if (!left.getType().equals(getLeftType())) {
			throw new IllegalArgumentException("Expected left %s to be of type %s, got %s instead".formatted(left,
					getLeftType().getName(), left.getType().getName()));
		}
		if (!right.getType().equals(getRightType())) {
			throw new IllegalArgumentException("Expected right %s to be of type %s, got %s instead".formatted(right,
					getRightType().getName(), right.getType().getName()));
		}
		this.left = left;
		this.right = right;
	}

	public abstract Class<T1> getLeftType();

	public abstract Class<T2> getRightType();

	public Term<T1> getLeft() {
		return left;
	}

	public Term<T2> getRight() {
		return right;
	}

	@Override
	public R evaluate(Valuation valuation) {
		var leftValue = left.evaluate(valuation);
		if (leftValue == null) {
			return null;
		}
		var rightValue = right.evaluate(valuation);
		if (rightValue == null) {
			return null;
		}
		return doEvaluate(leftValue, rightValue);
	}

	protected abstract R doEvaluate(T1 leftValue, T2 rightValue);

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (getClass() != other.getClass()) {
			return false;
		}
		var otherBinaryTerm = (BinaryTerm<?, ?, ?>) other;
		return left.equalsWithSubstitution(helper, otherBinaryTerm.left) && right.equalsWithSubstitution(helper,
				otherBinaryTerm.right);
	}

	@Override
	public Term<R> substitute(Substitution substitution) {
		return doSubstitute(substitution, left.substitute(substitution), right.substitute(substitution));
	}

	public abstract Term<R> doSubstitute(Substitution substitution, Term<T1> substitutedLeft,
										 Term<T2> substitutedRight);

	@Override
	public Set<AnyDataVariable> getInputVariables() {
		var inputVariables = new HashSet<>(left.getInputVariables());
		inputVariables.addAll(right.getInputVariables());
		return Collections.unmodifiableSet(inputVariables);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BinaryTerm<?, ?, ?> that = (BinaryTerm<?, ?, ?>) o;
		return left.equals(that.left) && right.equals(that.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), left, right);
	}
}
