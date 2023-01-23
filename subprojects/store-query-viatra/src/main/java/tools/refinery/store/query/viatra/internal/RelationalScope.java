package tools.refinery.store.query.viatra.internal;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.scope.IEngineContext;
import org.eclipse.viatra.query.runtime.api.scope.IIndexingErrorListener;
import org.eclipse.viatra.query.runtime.api.scope.QueryScope;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.viatra.internal.context.RelationalEngineContext;
import tools.refinery.store.query.viatra.internal.update.ModelUpdateListener;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.Collection;

public class RelationalScope extends QueryScope {
	private final Model model;

	private final ModelUpdateListener updateListener;

	public RelationalScope(Model model, Collection<AnyRelationView> relationViews) {
		this.model = model;
		updateListener = new ModelUpdateListener(model, relationViews);
	}

	@Override
	protected IEngineContext createEngineContext(ViatraQueryEngine engine, IIndexingErrorListener errorListener,
												 Logger logger) {
		return new RelationalEngineContext(model, updateListener);
	}
}
