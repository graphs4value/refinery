/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;

import java.util.Objects;

public abstract class ArithmeticBinaryTerm<T> extends BinaryTerm<T, T, T> {
	private final ArithmeticBinaryOperator operator;

	protected ArithmeticBinaryTerm(ArithmeticBinaryOperator operator, Term<T> left, Term<T> right) {
		super(left, right);
		this.operator = operator;
	}

	@Override
	public Class<T> getLeftType() {
		return getType();
	}

	@Override
	public Class<T> getRightType() {
		return getType();
	}

	public ArithmeticBinaryOperator getOperator() {
		return operator;
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherArithmeticBinaryTerm = (ArithmeticBinaryTerm<?>) other;
		return operator == otherArithmeticBinaryTerm.operator;
	}

	@Override
	public String toString() {
		return operator.formatString(getLeft().toString(), getRight().toString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		ArithmeticBinaryTerm<?> that = (ArithmeticBinaryTerm<?>) o;
		return operator == that.operator;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), operator);
	}
}
