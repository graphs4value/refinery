/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.localsearch;

import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.AbstractLocalSearchResultProvider;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchBackend;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchHints;
import org.eclipse.viatra.query.runtime.localsearch.plan.IPlanProvider;
import org.eclipse.viatra.query.runtime.localsearch.plan.SimplePlanProvider;
import org.eclipse.viatra.query.runtime.matchers.backend.IMatcherCapability;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackend;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryBackendContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;

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
