/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.localsearch;

import tools.refinery.viatra.runtime.localsearch.matcher.integration.AbstractLocalSearchResultProvider;
import tools.refinery.viatra.runtime.localsearch.matcher.integration.LocalSearchBackend;
import tools.refinery.viatra.runtime.localsearch.matcher.integration.LocalSearchHints;
import tools.refinery.viatra.runtime.localsearch.plan.IPlanProvider;
import tools.refinery.viatra.runtime.localsearch.plan.SimplePlanProvider;
import tools.refinery.viatra.runtime.matchers.backend.IMatcherCapability;
import tools.refinery.viatra.runtime.matchers.backend.IQueryBackend;
import tools.refinery.viatra.runtime.matchers.backend.IQueryBackendFactory;
import tools.refinery.viatra.runtime.matchers.backend.QueryEvaluationHint;
import tools.refinery.viatra.runtime.matchers.context.IQueryBackendContext;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PQuery;

public class RelationalLocalSearchBackendFactory implements IQueryBackendFactory {
	public static final RelationalLocalSearchBackendFactory INSTANCE = new RelationalLocalSearchBackendFactory();

	private RelationalLocalSearchBackendFactory() {
	}

	@Override
	public IQueryBackend create(IQueryBackendContext context) {
		return new LocalSearchBackend(context) {
			// Create a new {@link IPlanProvider}, because the original {@link LocalSearchBackend#planProvider} is not
			// accessible.
			private final IPlanProvider planProvider = new SimplePlanProvider(context.getLogger());

			@Override
			protected AbstractLocalSearchResultProvider initializeResultProvider(PQuery query,
																				 QueryEvaluationHint hints) {
				return new RelationalLocalSearchResultProvider(this, context, query, planProvider, hints);
			}

			@Override
			public IQueryBackendFactory getFactory() {
				return RelationalLocalSearchBackendFactory.this;
			}
		};
	}

	@Override
	public Class<? extends IQueryBackend> getBackendClass() {
		return LocalSearchBackend.class;
	}

	@Override
	public IMatcherCapability calculateRequiredCapability(PQuery pQuery, QueryEvaluationHint queryEvaluationHint) {
		return LocalSearchHints.parse(queryEvaluationHint);
	}

	@Override
	public boolean isCaching() {
		return false;
	}
}
