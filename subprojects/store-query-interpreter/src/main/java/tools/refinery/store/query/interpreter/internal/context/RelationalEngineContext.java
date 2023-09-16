/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.context;

import tools.refinery.interpreter.api.scope.IBaseIndex;
import tools.refinery.interpreter.api.scope.IEngineContext;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.store.query.interpreter.internal.QueryInterpreterAdapterImpl;

public class RelationalEngineContext implements IEngineContext {
	private final IBaseIndex baseIndex = new DummyBaseIndexer();
	private final RelationalRuntimeContext runtimeContext;

	public RelationalEngineContext(QueryInterpreterAdapterImpl adapter) {
		runtimeContext = new RelationalRuntimeContext(adapter);
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
