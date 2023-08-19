/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.localsearch;

import tools.refinery.viatra.runtime.localsearch.matcher.integration.AbstractLocalSearchResultProvider;
import tools.refinery.viatra.runtime.localsearch.matcher.integration.LocalSearchBackend;
import tools.refinery.viatra.runtime.localsearch.matcher.integration.LocalSearchHints;
import tools.refinery.viatra.runtime.localsearch.plan.IPlanProvider;
import tools.refinery.viatra.runtime.localsearch.planner.compiler.IOperationCompiler;
import tools.refinery.viatra.runtime.matchers.backend.QueryEvaluationHint;
import tools.refinery.viatra.runtime.matchers.context.IQueryBackendContext;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PQuery;

class RelationalLocalSearchResultProvider extends AbstractLocalSearchResultProvider {
	public RelationalLocalSearchResultProvider(LocalSearchBackend backend, IQueryBackendContext context, PQuery query,
											   IPlanProvider planProvider, QueryEvaluationHint userHints) {
		super(backend, context, query, planProvider, userHints);
	}

	@Override
	protected IOperationCompiler getOperationCompiler(IQueryBackendContext backendContext,
													  LocalSearchHints configuration) {
		return new RelationalOperationCompiler(runtimeContext);
	}
}
