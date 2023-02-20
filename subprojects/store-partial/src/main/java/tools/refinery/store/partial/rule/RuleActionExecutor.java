package tools.refinery.store.partial.rule;

import tools.refinery.store.partial.MergeResult;
import tools.refinery.store.tuple.TupleLike;

@FunctionalInterface
public interface RuleActionExecutor {
	MergeResult execute(TupleLike activationTuple);
}
