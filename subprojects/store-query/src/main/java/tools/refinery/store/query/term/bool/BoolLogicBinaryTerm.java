/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.bool;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.*;

import java.util.Objects;

public class BoolLogicBinaryTerm extends BinaryTerm<Boolean, Boolean, Boolean> {
	private final LogicBinaryOperator operator;

	protected BoolLogicBinaryTerm(LogicBinaryOperator operator, Term<Boolean> left, Term<Boolean> right) {
		super(left, right);
		this.operator = operator;
	}

	@Override
	public Class<Boolean> getType() {
		return Boolean.class;
	}

	@Override
	public Class<Boolean> getLeftType() {
		return getType();
	}

	@Override
	public Class<Boolean> getRightType() {
		return getType();
	}

	public LogicBinaryOperator getOperator() {
		return operator;
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherBoolLogicBinaryTerm = (BoolLogicBinaryTerm) other;
		return operator == otherBoolLogicBinaryTerm.operator;
	}

	@Override
	public Term<Boolean> doSubstitute(Substitution substitution, Term<Boolean> substitutedLeft,
									  Term<Boolean> substitutedRight) {
		return new BoolLogicBinaryTerm(getOperator(), substitutedLeft, substitutedRight);
	}

	@Override
	protected Boolean doEvaluate(Boolean leftValue, Boolean rightValue) {
		return switch (getOperator()) {
			case AND -> leftValue && rightValue;
			case OR -> leftValue || rightValue;
			case XOR -> leftValue ^ rightValue;
		};
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
		BoolLogicBinaryTerm that = (BoolLogicBinaryTerm) o;
		return operator == that.operator;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), operator);
	}
}
