/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.equality.LiteralHashCodeHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Variable;

import java.util.Objects;
import java.util.Set;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public final class EquivalenceLiteral extends AbstractLiteral implements CanNegate<EquivalenceLiteral> {
	private final boolean positive;
	private final Variable left;
	private final Variable right;

	public EquivalenceLiteral(boolean positive, Variable left, Variable right) {
		if (!left.tryGetType().equals(right.tryGetType())) {
			throw new InvalidQueryException("Variables %s and %s of different type cannot be equivalent"
					.formatted(left, right));
		}
		this.positive = positive;
		this.left = left;
		this.right = right;
	}

	public boolean isPositive() {
		return positive;
	}

	public Variable getLeft() {
		return left;
	}

	public Variable getRight() {
		return right;
	}

	@Override
	public Set<Variable> getOutputVariables() {
		return Set.of(left);
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Set.of(right);
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Set.of();
	}

	@Override
	public EquivalenceLiteral negate() {
		return new EquivalenceLiteral(!positive, left, right);
	}

	@Override
	public EquivalenceLiteral substitute(Substitution substitution) {
		return new EquivalenceLiteral(positive, substitution.getSubstitute(left),
				substitution.getSubstitute(right));
	}

	@Override
	public Literal reduce() {
		if (left.equals(right)) {
			return positive ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
		}
		return this;
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other.getClass() != getClass()) {
			return false;
		}
		var otherEquivalenceLiteral = (EquivalenceLiteral) other;
		return helper.variableEqual(left, otherEquivalenceLiteral.left) &&
				helper.variableEqual(right, otherEquivalenceLiteral.right);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), helper.getVariableHashCode(left),
				helper.getVariableHashCode(right));
	}

	@Override
	public String toString() {
		return "%s %s %s".formatted(left, positive ? "===" : "!==", right);
	}
}
