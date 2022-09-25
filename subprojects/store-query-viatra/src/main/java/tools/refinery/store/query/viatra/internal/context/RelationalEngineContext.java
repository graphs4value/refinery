package tools.refinery.store.query.viatra.internal.context;

import org.eclipse.viatra.query.runtime.api.scope.IBaseIndex;
import org.eclipse.viatra.query.runtime.api.scope.IEngineContext;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContext;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.viatra.internal.viewupdate.ModelUpdateListener;

public class RelationalEngineContext implements IEngineContext {
	private final IBaseIndex baseIndex = new DummyBaseIndexer();
	private final RelationalRuntimeContext runtimeContext;

	public RelationalEngineContext(Model model, ModelUpdateListener updateListener) {
		runtimeContext = new RelationalRuntimeContext(model, updateListener);
	}

	@Override
	public IBaseIndex getBaseIndex() {
		return this.baseIndex;
	}

	@Override
	public void dispose() {
		// Nothing to dispose, because lifecycle is not controlled by the engine.
	}

	@Override
	public IQueryRuntimeContext getQueryRuntimeContext() {
		return runtimeContext;
	}
}
