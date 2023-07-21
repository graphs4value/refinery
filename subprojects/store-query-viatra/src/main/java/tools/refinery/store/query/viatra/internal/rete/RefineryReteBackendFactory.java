/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.query.viatra.internal.rete;

import org.eclipse.viatra.query.runtime.matchers.backend.*;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryBackendContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.rete.construction.plancompiler.ReteRecipeCompiler;
import org.eclipse.viatra.query.runtime.rete.matcher.IncrementalMatcherCapability;
import org.eclipse.viatra.query.runtime.rete.matcher.ReteEngine;
import org.eclipse.viatra.query.runtime.rete.matcher.TimelyConfiguration;
import org.eclipse.viatra.query.runtime.rete.util.Options;

// Singleton implementations follows the VIATRA implementation.
@SuppressWarnings("squid:S6548")
public class RefineryReteBackendFactory implements IQueryBackendFactory {
	/**
	 * EXPERIMENTAL
	 */
	protected static final int RETE_THREADS = 0;

	/**
	 * @since 2.0
	 */
	public static final RefineryReteBackendFactory INSTANCE = new RefineryReteBackendFactory();

	/**
	 * @since 1.5
	 */
	@Override
	public IQueryBackend create(IQueryBackendContext context) {
		return create(context, false, null);
	}

	/**
	 * @since 2.4
	 */
	public IQueryBackend create(IQueryBackendContext context, boolean deleteAndReDeriveEvaluation,
								TimelyConfiguration timelyConfiguration) {
		ReteEngine engine;
		engine = new RefineryReteEngine(context, RETE_THREADS, deleteAndReDeriveEvaluation, timelyConfiguration);
		IQueryBackendHintProvider hintConfiguration = engine.getHintConfiguration();
		ReteRecipeCompiler compiler = new RefineryReteRecipeCompiler(
				Options.builderMethod.layoutStrategy(context, hintConfiguration), context.getLogger(),
				context.getRuntimeContext().getMetaContext(), context.getQueryCacheContext(), hintConfiguration,
				context.getQueryAnalyzer(), deleteAndReDeriveEvaluation, timelyConfiguration);
		engine.setCompiler(compiler);
		return engine;
	}

	@Override
	public Class<? extends IQueryBackend> getBackendClass() {
		return ReteEngine.class;
	}

	@Override
	public int hashCode() {
		return RefineryReteBackendFactory.class.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		return obj instanceof RefineryReteBackendFactory;
	}

	/**
	 * @since 1.4
	 */
	@Override
	public IMatcherCapability calculateRequiredCapability(PQuery query, QueryEvaluationHint hint) {
		return new IncrementalMatcherCapability();
	}

	@Override
	public boolean isCaching() {
		return true;
	}
}
