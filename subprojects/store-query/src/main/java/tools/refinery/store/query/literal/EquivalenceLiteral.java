/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.NodeVariable;

import java.util.Objects;

public final class EquivalenceLiteral
		implements CanNegate<EquivalenceLiteral> {
	private final boolean positive;
	private final NodeVariable left;
	private final NodeVariable right;
	private final VariableBinder variableBinder;

	public EquivalenceLiteral(boolean positive, NodeVariable left, NodeVariable right) {
		this.positive = positive;
		this.left = left;
		this.right = right;
		variableBinder = VariableBinder.builder()
				.variable(left, positive ? VariableDirection.IN_OUT : VariableDirection.IN)
				.variable(right, VariableDirection.IN)
				.build();
	}

	public boolean isPositive() {
		return positive;
	}

	public NodeVariable getLeft() {
		return left;
	}

	public NodeVariable getRight() {
		return right;
	}

	@Override
	public VariableBinder getVariableBinder() {
		return variableBinder;
	}

	@Override
	public EquivalenceLiteral negate() {
		return new EquivalenceLiteral(!positive, left, right);
	}

	@Override
	public EquivalenceLiteral substitute(Substitution substitution) {
		return new EquivalenceLiteral(positive, substitution.getTypeSafeSubstitute(left),
				substitution.getTypeSafeSubstitute(right));
	}

	@Override
	public LiteralReduction getReduction() {
		if (left.equals(right)) {
			return positive ? LiteralReduction.ALWAYS_TRUE : LiteralReduction.ALWAYS_FALSE;
		}
		return LiteralReduction.NOT_REDUCIBLE;
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other.getClass() != getClass()) {
			return false;
		}
		var otherEquivalenceLiteral = (EquivalenceLiteral) other;
		return helper.variableEqual(left, otherEquivalenceLiteral.left) && helper.variableEqual(right,
				otherEquivalenceLiteral.right);
	}

	@Override
	public String toString() {
		return "%s %s %s".formatted(left, positive ? "===" : "!==", right);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (EquivalenceLiteral) obj;
		return this.positive == that.positive &&
				Objects.equals(this.left, that.left) &&
				Objects.equals(this.right, that.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), positive, left, right);
	}
}
