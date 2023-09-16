/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.api.InterpreterEngine;
import tools.refinery.interpreter.api.scope.IEngineContext;
import tools.refinery.interpreter.api.scope.IIndexingErrorListener;
import tools.refinery.interpreter.api.scope.QueryScope;
import tools.refinery.store.query.interpreter.internal.context.RelationalEngineContext;

public class RelationalScope extends QueryScope {
	private final QueryInterpreterAdapterImpl adapter;

	public RelationalScope(QueryInterpreterAdapterImpl adapter) {
		this.adapter = adapter;
	}

	@Override
	protected IEngineContext createEngineContext(InterpreterEngine engine, IIndexingErrorListener errorListener,
												 Logger logger) {
		return new RelationalEngineContext(adapter);
	}
}
