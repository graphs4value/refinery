package tools.refinery.store.query.atom;

import tools.refinery.store.query.Variable;

import java.util.Set;

public record ConstantAtom(Variable variable, int nodeId) implements DNFAtom {
	@Override
	public void collectAllVariables(Set<Variable> variables) {
		variables.add(variable);
	}
}
