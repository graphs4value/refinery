/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.rule;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.MergeResult;
import tools.refinery.store.tuple.Tuple;

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

	public MergeResult execute(Tuple activationTuple) {
		MergeResult mergeResult = MergeResult.UNCHANGED;
		for (var actionExecutor : actionExecutors) {
			mergeResult = mergeResult.andAlso(actionExecutor.execute(activationTuple));
		}
		return mergeResult;
	}
}
