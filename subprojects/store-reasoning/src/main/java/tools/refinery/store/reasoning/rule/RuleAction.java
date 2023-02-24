package tools.refinery.store.reasoning.rule;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.Variable;

import java.util.List;

public interface RuleAction {
	List<Variable> arguments();

	RuleActionExecutor createExecutor(int[] argumentIndices, Model model);
}
