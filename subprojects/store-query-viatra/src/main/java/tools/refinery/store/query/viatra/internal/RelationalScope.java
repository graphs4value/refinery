/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal;

import org.apache.log4j.Logger;
import tools.refinery.viatra.runtime.api.ViatraQueryEngine;
import tools.refinery.viatra.runtime.api.scope.IEngineContext;
import tools.refinery.viatra.runtime.api.scope.IIndexingErrorListener;
import tools.refinery.viatra.runtime.api.scope.QueryScope;
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
