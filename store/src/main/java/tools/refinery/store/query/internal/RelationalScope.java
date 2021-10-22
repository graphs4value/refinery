package tools.refinery.store.query.internal;

import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.scope.IEngineContext;
import org.eclipse.viatra.query.runtime.api.scope.IIndexingErrorListener;
import org.eclipse.viatra.query.runtime.api.scope.QueryScope;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.query.view.RelationView;

public class RelationalScope extends QueryScope{
	private final Model model;
	private final ModelUpdateListener updateListener;
	
	public RelationalScope(Model model, Set<RelationView<?>> relationViews) {
		this.model = model;
		this.updateListener = new ModelUpdateListener(relationViews);
		//this.changeListener = new 
	}
	
	public <D> void processUpdate(Relation<D> relation, Tuple key, D oldValue, D newValue) {
		updateListener.addUpdate(relation, key, oldValue, newValue);
	}
	
	public boolean hasChange() {
		return updateListener.hasChange();
	}

	public void flush() {
		updateListener.flush();
	}
	
	@Override
	protected IEngineContext createEngineContext(ViatraQueryEngine engine, IIndexingErrorListener errorListener,
			Logger logger) {
		return new RelationalEngineContext(model, updateListener);
	}
}
