package tools.refinery.store.query.viatra.internal.localsearch;

import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.AbstractLocalSearchResultProvider;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchBackend;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchHints;
import org.eclipse.viatra.query.runtime.localsearch.plan.IPlanProvider;
import org.eclipse.viatra.query.runtime.localsearch.planner.compiler.IOperationCompiler;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryBackendContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;

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
