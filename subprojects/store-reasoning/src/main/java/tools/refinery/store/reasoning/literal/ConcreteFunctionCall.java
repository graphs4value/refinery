package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.literal.AbstractCallLiteral;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.Variable;

import java.util.List;
import java.util.Set;

public class ConcreteFunctionCall extends AbstractCallLiteral {
	protected ConcreteFunctionCall(Constraint target, List<Variable> arguments) {
		super(target, arguments);
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return null;
	}

	@Override
	public AbstractCallLiteral withArguments(Constraint newTarget, List<Variable> newArguments) {
		return null;
	}

	@Override
	public Set<Variable> getOutputVariables() {
		return Set.of();
	}
}
