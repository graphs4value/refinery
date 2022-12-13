package tools.refinery.store.query.viatra.internal;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.scope.IEngineContext;
import org.eclipse.viatra.query.runtime.api.scope.IIndexingErrorListener;
import org.eclipse.viatra.query.runtime.api.scope.QueryScope;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.query.viatra.internal.context.RelationalEngineContext;
import tools.refinery.store.query.viatra.internal.viewupdate.ModelUpdateListener;
import tools.refinery.store.query.view.AnyRelationView;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

public class RelationalScope extends QueryScope {
	private final Model model;

	private final ModelUpdateListener updateListener;

	public RelationalScope(Model model, Set<AnyRelationView> relationViews) {
		this.model = model;
		this.updateListener = new ModelUpdateListener(relationViews);
	}

	public <D> void processUpdate(Relation<D> relation, Tuple key, D oldValue, D newValue) {
		updateListener.addUpdate(relation, key, oldValue, newValue);
	}

	public boolean hasChanges() {
		return updateListener.hasChanges();
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
