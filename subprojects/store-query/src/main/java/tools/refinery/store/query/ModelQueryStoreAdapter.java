/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.view.AnySymbolView;

import java.util.Collection;

public interface ModelQueryStoreAdapter extends ModelStoreAdapter {
	Collection<AnySymbolView> getSymbolViews();

	Collection<AnyQuery> getQueries();

	default AnyQuery getCanonicalQuery(AnyQuery query) {
		return getCanonicalQuery((Query<?>) query);
	}

	<T> Query<T> getCanonicalQuery(Query<T> query);

	@Override
	ModelQueryAdapter createModelAdapter(Model model);
}
