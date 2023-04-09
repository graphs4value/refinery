/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;

import java.util.Set;

public record EquivalenceLiteral(boolean positive, NodeVariable left, NodeVariable right)
		implements CanNegate<EquivalenceLiteral> {
	@Override
	public Set<Variable> getBoundVariables() {
		// If one side of a {@code positive} equivalence is bound, it may bind its other side, but we under-approximate
		// this behavior by not binding any of the sides by default.
		return Set.of();
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
}
