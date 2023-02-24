package tools.refinery.store.reasoning.rule;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.Dnf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public record Rule(Dnf precondition, List<RuleAction> actions) {
	public Rule {
		var parameterSet = new HashSet<>(precondition.getParameters());
		for (var action : actions) {
			for (var argument : action.arguments()) {
				if (!parameterSet.contains(argument)) {
					throw new IllegalArgumentException(
							"Argument %s of action %s does not appear in the parameter list %s of %s"
									.formatted(argument, action, precondition.getParameters(), precondition.name()));
				}
			}
		}
	}

	public RuleExecutor createExecutor(Model model) {
		var parameters = precondition.getParameters();
		var actionExecutors = new ArrayList<RuleActionExecutor>(actions.size());
		for (var action : actions) {
			var arguments = action.arguments();
			int arity = arguments.size();
			var argumentIndices = new int[arity];
			for (int i = 0; i < arity; i++) {
				argumentIndices[i] = parameters.indexOf(arguments.get(i));
			}
			actionExecutors.add(action.createExecutor(argumentIndices, model));
		}
		return new RuleExecutor(this, model, actionExecutors);
	}
}
