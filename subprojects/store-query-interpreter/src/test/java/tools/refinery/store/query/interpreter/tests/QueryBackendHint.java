/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.tests;

import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;

/**
 * Overrides {@link QueryEvaluationHint#toString()} for pretty names in parametric test names.
 */
class QueryBackendHint extends QueryEvaluationHint {
	public QueryBackendHint(BackendRequirement backendRequirementType) {
		super(null, backendRequirementType);
	}

	@Override
	public String toString() {
		return switch (getQueryBackendRequirementType()) {
			case UNSPECIFIED -> "default";
			case DEFAULT_CACHING -> "incremental";
			case DEFAULT_SEARCH -> "localSearch";
			default -> throw new IllegalStateException("Unknown BackendRequirement");
		};
	}
}
