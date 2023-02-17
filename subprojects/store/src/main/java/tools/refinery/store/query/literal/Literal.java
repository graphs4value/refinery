package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;

import java.util.Map;
import java.util.Set;

public interface Literal {
	void collectAllVariables(Set<Variable> variables);

	Literal substitute(Map<Variable, Variable> substitution);
}
