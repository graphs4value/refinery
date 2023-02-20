package tools.refinery.store.partial.rule;

import tools.refinery.store.partial.MergeResult;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.TupleLike;

import java.util.List;

public final class RuleExecutor {
	private final Rule rule;
	private final Model model;
	private final List<RuleActionExecutor> actionExecutors;

	RuleExecutor(Rule rule, Model model, List<RuleActionExecutor> actionExecutors) {
		this.rule = rule;
		this.model = model;
		this.actionExecutors = actionExecutors;
	}

	public Rule getRule() {
		return rule;
	}

	public Model getModel() {
		return model;
	}
	public MergeResult execute(TupleLike activationTuple) {
		MergeResult mergeResult = MergeResult.UNCHANGED;
		for (var actionExecutor : actionExecutors) {
			mergeResult = mergeResult.andAlso(actionExecutor.execute(activationTuple));
		}
		return mergeResult;
	}
}
