package tools.refinery.store.query;

import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.scope.IEngineContext;
import org.eclipse.viatra.query.runtime.api.scope.IIndexingErrorListener;
import org.eclipse.viatra.query.runtime.api.scope.QueryScope;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.query.internal.RelationUpdateListener;
import tools.refinery.store.query.internal.RelationalEngineContext;
import tools.refinery.store.query.view.RelationView;

public class RelationalScope extends QueryScope{
	private final Model model;
	private final RelationUpdateListener updateListener;
	
	public RelationalScope(Model model, Set<RelationView<?>> relationViews) {
		this.model = model;
		updateListener = new RelationUpdateListener(relationViews);
	}
	
	public <D> void processUpdate(RelationView<D> relationView, Tuple key, D oldValue, D newValue) {
		updateListener.processChange(relationView, key, oldValue, newValue);
	}

	@Override
	protected IEngineContext createEngineContext(ViatraQueryEngine engine, IIndexingErrorListener errorListener,
			Logger logger) {
		return new RelationalEngineContext(model, updateListener);
	}
}
