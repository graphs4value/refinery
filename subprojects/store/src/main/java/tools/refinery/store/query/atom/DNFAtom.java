package tools.refinery.store.query.atom;

import tools.refinery.store.query.Variable;

import java.util.Set;

public interface DNFAtom {
	void collectAllVariables(Set<Variable> variables);
}
