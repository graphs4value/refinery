/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.tests;

import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class QueryEvaluationHintSource implements ArgumentsProvider {
	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
		return Stream.of(
				Arguments.of(new QueryBackendHint(QueryEvaluationHint.BackendRequirement.UNSPECIFIED)),
				Arguments.of(new QueryBackendHint(QueryEvaluationHint.BackendRequirement.DEFAULT_CACHING)),
				Arguments.of(new QueryBackendHint(QueryEvaluationHint.BackendRequirement.DEFAULT_SEARCH))
		);
	}
}
