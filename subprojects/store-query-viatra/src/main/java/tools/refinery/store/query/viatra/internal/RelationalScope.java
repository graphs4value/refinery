package tools.refinery.store.query.viatra.internal;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.scope.IEngineContext;
import org.eclipse.viatra.query.runtime.api.scope.IIndexingErrorListener;
import org.eclipse.viatra.query.runtime.api.scope.QueryScope;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.viatra.internal.context.RelationalEngineContext;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.Map;

public class RelationalScope extends QueryScope {
	private final Model model;
	private final Map<AnyRelationView, IInputKey> relationViews;

	public RelationalScope(Model model, Map<AnyRelationView, IInputKey> relationViews) {
		this.model = model;
		this.relationViews = relationViews;
	}

	@Override
	protected IEngineContext createEngineContext(ViatraQueryEngine engine, IIndexingErrorListener errorListener,
												 Logger logger) {
		return new RelationalEngineContext(model, relationViews);
	}
}
