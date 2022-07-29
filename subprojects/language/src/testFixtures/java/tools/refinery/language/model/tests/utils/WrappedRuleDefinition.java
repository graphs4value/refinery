package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.RuleDefinition;

public record WrappedRuleDefinition(RuleDefinition ruleDefinition) implements WrappedParametricDefinition {
	@Override
	public RuleDefinition get() {
		return ruleDefinition;
	}
	
	public WrappedActionLiteral actionLit(int i) {
		return new WrappedActionLiteral(ruleDefinition.getAction().getActionLiterals().get(i));
	}
}
