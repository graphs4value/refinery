/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.resultset.AnyResultSet;
import tools.refinery.store.query.resultset.ResultSet;

public interface ModelQueryAdapter extends ModelAdapter {
	ModelQueryStoreAdapter getStoreAdapter();

	default AnyResultSet getResultSet(AnyQuery query) {
		return getResultSet((Query<?>) query);
	}

	<T> ResultSet<T> getResultSet(Query<T> query);

	boolean hasPendingChanges();

	void flushChanges();
}
