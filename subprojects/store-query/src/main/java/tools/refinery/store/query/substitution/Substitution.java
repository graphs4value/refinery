package tools.refinery.store.query.substitution;

import tools.refinery.store.query.Variable;

@FunctionalInterface
public interface Substitution {
	Variable getSubstitute(Variable variable);
}
