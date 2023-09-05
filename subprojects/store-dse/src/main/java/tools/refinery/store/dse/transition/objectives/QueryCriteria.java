/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.objectives;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.dnf.AnyQuery;

public class QueryCriteria implements Criterion {
	protected final boolean acceptIfHasMatch;
	protected final AnyQuery query;

	/**
	 * Criteria based on the existence of matches evaluated on the model.
	 * @param query The query evaluated on the model.
	 * @param acceptIfHasMatch If true, the criteria satisfied if the query has any match on the model. Otherwise,
	 *                            the criteria satisfied if the query has no match on the model.
	 */
	public QueryCriteria(AnyQuery query, boolean acceptIfHasMatch) {
		this.query = query;
		this.acceptIfHasMatch = acceptIfHasMatch;
	}

	@Override
	public CriterionCalculator createCalculator(Model model) {
		var resultSet = model.getAdapter(ModelQueryAdapter.class).getResultSet(query);
		if(acceptIfHasMatch) {
			return () -> resultSet.size() > 0;
		} else {
			return () -> resultSet.size() == 0;
		}
	}

	@Override
	public void doConfigure(ModelStoreBuilder storeBuilder) {
		Criterion.super.doConfigure(storeBuilder);
		storeBuilder.getAdapter(ModelQueryBuilder.class).query(query);
	}
}
