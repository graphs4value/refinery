package tools.refinery.store.reasoning.rule;

import tools.refinery.store.reasoning.MergeResult;
import tools.refinery.store.tuple.Tuple;

@FunctionalInterface
public interface RuleActionExecutor {
	MergeResult execute(Tuple activationTuple);
}
