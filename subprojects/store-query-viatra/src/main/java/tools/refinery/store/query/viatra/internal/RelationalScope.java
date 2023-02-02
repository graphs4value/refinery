package tools.refinery.store.query.viatra.internal;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.scope.IEngineContext;
import org.eclipse.viatra.query.runtime.api.scope.IIndexingErrorListener;
import org.eclipse.viatra.query.runtime.api.scope.QueryScope;
import tools.refinery.store.query.viatra.internal.context.RelationalEngineContext;

public class RelationalScope extends QueryScope {
	private final ViatraModelQueryAdapterImpl adapter;

	public RelationalScope(ViatraModelQueryAdapterImpl adapter) {
		this.adapter = adapter;
	}

	@Override
	protected IEngineContext createEngineContext(ViatraQueryEngine engine, IIndexingErrorListener errorListener,
												 Logger logger) {
		return new RelationalEngineContext(adapter);
	}
}
