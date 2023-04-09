/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.PredicateDefinition;

public record WrappedPredicateDefinition(PredicateDefinition predicateDefinition)
		implements WrappedParametricDefinition {
	@Override
	public PredicateDefinition get() {
		return predicateDefinition;
	}

	public WrappedConjunction conj(int i) {
		return new WrappedConjunction(predicateDefinition.getBodies().get(i));
	}
}
