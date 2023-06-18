/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.rule;

import tools.refinery.store.reasoning.MergeResult;
import tools.refinery.store.tuple.Tuple;

@FunctionalInterface
public interface RuleActionExecutor {
	MergeResult execute(Tuple activationTuple);
}
