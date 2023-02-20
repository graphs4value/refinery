package tools.refinery.store.query.literal;

import tools.refinery.store.query.DnfUtils;
import tools.refinery.store.query.Variable;

import java.util.Map;
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
	public EquivalenceLiteral substitute(Map<Variable, Variable> substitution) {
		return new EquivalenceLiteral(positive, DnfUtils.maybeSubstitute(left, substitution),
				DnfUtils.maybeSubstitute(right, substitution));
	}

	@Override
	public LiteralReduction getReduction() {
		if (left.equals(right)) {
			return positive ? LiteralReduction.ALWAYS_TRUE : LiteralReduction.ALWAYS_FALSE;
		}
		return LiteralReduction.NOT_REDUCIBLE;
	}
}
