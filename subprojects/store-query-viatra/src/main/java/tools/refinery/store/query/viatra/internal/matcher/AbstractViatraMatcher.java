/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.matcher;

import tools.refinery.viatra.runtime.matchers.backend.IQueryResultProvider;
import tools.refinery.viatra.runtime.matchers.backend.IUpdateable;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.resultset.AbstractResultSet;
import tools.refinery.store.query.viatra.internal.ViatraModelQueryAdapterImpl;

public abstract class AbstractViatraMatcher<T> extends AbstractResultSet<T> implements IUpdateable {
	protected final IQueryResultProvider backend;

	protected AbstractViatraMatcher(ViatraModelQueryAdapterImpl adapter, Query<T> query,
									RawPatternMatcher rawPatternMatcher) {
		super(adapter, query);
		backend = rawPatternMatcher.getBackend();
	}

	@Override
	protected void startListeningForChanges() {
		backend.addUpdateListener(this, this, false);
	}

	@Override
	protected void stopListeningForChanges() {
		backend.removeUpdateListener(this);
	}
}
