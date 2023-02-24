package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;

import java.util.Set;

public record ConstantLiteral(Variable variable, int nodeId) implements Literal {
	@Override
	public void collectAllVariables(Set<Variable> variables) {
		variables.add(variable);
	}

	@Override
	public ConstantLiteral substitute(Substitution substitution) {
		return new ConstantLiteral(substitution.getSubstitute(variable), nodeId);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other.getClass() != getClass()) {
			return false;
		}
		var otherConstantLiteral = (ConstantLiteral) other;
		return helper.variableEqual(variable, otherConstantLiteral.variable) && nodeId == otherConstantLiteral.nodeId;
	}

	@Override
	public String toString() {
		return "%s === @Constant %d".formatted(variable, nodeId);
	}
}
