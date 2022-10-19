package tools.refinery.store.query.atom;

import tools.refinery.store.query.Variable;

import java.util.Set;

public record EquivalenceAtom(boolean positive, Variable left, Variable right) implements DNFAtom {
	public EquivalenceAtom(Variable left, Variable right) {
		this(true, left, right);
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		variables.add(left);
		variables.add(right);
	}
}
