package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;

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
}
