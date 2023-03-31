package tools.refinery.store.query.literal;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;

import java.util.Set;

public record ConstantLiteral(NodeVariable variable, int nodeId) implements Literal {
	@Override
	public Set<Variable> getBoundVariables() {
		return Set.of(variable);
	}

	@Override
	public ConstantLiteral substitute(Substitution substitution) {
		return new ConstantLiteral(substitution.getTypeSafeSubstitute(variable), nodeId);
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
