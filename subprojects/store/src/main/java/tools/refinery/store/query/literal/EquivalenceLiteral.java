package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;

import java.util.Set;

public record EquivalenceLiteral(boolean positive, Variable left, Variable right) implements Literal {
	public EquivalenceLiteral(Variable left, Variable right) {
		this(true, left, right);
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		variables.add(left);
		variables.add(right);
	}
}
