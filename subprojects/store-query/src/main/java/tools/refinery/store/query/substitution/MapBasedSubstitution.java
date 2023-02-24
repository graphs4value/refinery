package tools.refinery.store.query.substitution;

import tools.refinery.store.query.Variable;

import java.util.Map;

public record MapBasedSubstitution(Map<Variable, Variable> map, Substitution fallback) implements Substitution {
	@Override
	public Variable getSubstitute(Variable variable) {
		var value = map.get(variable);
		return value == null ? fallback.getSubstitute(variable) : value;
	}
}
