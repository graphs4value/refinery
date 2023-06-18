/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.RuleDefinition;

public record WrappedRuleDefinition(RuleDefinition ruleDefinition) implements WrappedParametricDefinition {
	@Override
	public RuleDefinition get() {
		return ruleDefinition;
	}

	public WrappedConjunction conj(int i) {
		return new WrappedConjunction(ruleDefinition.getPreconditions().get(i));
	}

	public WrappedConsequent consequent(int i) {
		return new WrappedConsequent(ruleDefinition.getConsequents().get(i));
	}
}
