package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;

import java.util.Set;

public record EquivalenceLiteral(boolean positive, Variable left, Variable right)
		implements PolarLiteral<EquivalenceLiteral> {
	@Override
	public void collectAllVariables(Set<Variable> variables) {
		variables.add(left);
		variables.add(right);
	}

	@Override
	public EquivalenceLiteral negate() {
		return new EquivalenceLiteral(!positive, left, right);
	}

	@Override
	public EquivalenceLiteral substitute(Substitution substitution) {
		return new EquivalenceLiteral(positive, substitution.getSubstitute(left), substitution.getSubstitute(right));
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
