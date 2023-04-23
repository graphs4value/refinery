/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.valuation.Valuation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class BinaryTerm<R, T1, T2> extends AbstractTerm<R> {
	private final Class<T1> leftType;
	private final Class<T2> rightType;
	private final Term<T1> left;
	private final Term<T2> right;

	protected BinaryTerm(Class<R> type, Class<T1> leftType, Class<T2> rightType, Term<T1> left, Term<T2> right) {
		super(type);
		if (!left.getType().equals(leftType)) {
			throw new IllegalArgumentException("Expected left %s to be of type %s, got %s instead".formatted(
					left, leftType.getName(), left.getType().getName()));
		}
		if (!right.getType().equals(rightType)) {
			throw new IllegalArgumentException("Expected right %s to be of type %s, got %s instead".formatted(
					right, rightType.getName(), right.getType().getName()));
		}
		this.leftType = leftType;
		this.rightType = rightType;
		this.left = left;
		this.right = right;
	}

	public Class<T1> getLeftType() {
		return leftType;
	}

	public Class<T2> getRightType() {
		return rightType;
	}

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
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherBinaryTerm = (BinaryTerm<?, ?, ?>) other;
		return leftType.equals(otherBinaryTerm.leftType) &&
				rightType.equals(otherBinaryTerm.rightType) &&
				left.equalsWithSubstitution(helper, otherBinaryTerm.left) &&
				right.equalsWithSubstitution(helper, otherBinaryTerm.right);
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
		if (!super.equals(o)) return false;
		BinaryTerm<?, ?, ?> that = (BinaryTerm<?, ?, ?>) o;
		return Objects.equals(leftType, that.leftType) &&
				Objects.equals(rightType, that.rightType) &&
				Objects.equals(left, that.left) &&
				Objects.equals(right, that.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), leftType, rightType, left, right);
	}
}
