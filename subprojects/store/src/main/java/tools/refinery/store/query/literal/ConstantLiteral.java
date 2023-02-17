package tools.refinery.store.query.literal;

import tools.refinery.store.query.DnfUtils;
import tools.refinery.store.query.Variable;

import java.util.Map;
import java.util.Set;

public record ConstantLiteral(Variable variable, int nodeId) implements Literal {
	@Override
	public void collectAllVariables(Set<Variable> variables) {
		variables.add(variable);
	}

	@Override
	public ConstantLiteral substitute(Map<Variable, Variable> substitution) {
		return new ConstantLiteral(DnfUtils.maybeSubstitute(variable, substitution), nodeId);
	}
}
