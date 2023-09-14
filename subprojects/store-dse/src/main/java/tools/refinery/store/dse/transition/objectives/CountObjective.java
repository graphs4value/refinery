/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.objectives;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.ModelQueryStoreAdapter;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.literal.Reduction;

public class CountObjective implements Objective {
	private final RelationalQuery query;
	private final double weight;

	public CountObjective(RelationalQuery query) {
		this(query, 1);
	}

	public CountObjective(RelationalQuery query, double weight) {
		this.query = query;
		this.weight = weight;
	}

	@Override
	public boolean isAlwaysZero(ModelStore store) {
		var queryStore = store.getAdapter(ModelQueryStoreAdapter.class);
		var canonicalQuery = queryStore.getCanonicalQuery(query);
		return canonicalQuery.getDnf().getReduction() == Reduction.ALWAYS_FALSE;
	}

	@Override
	public ObjectiveCalculator createCalculator(Model model) {
		var resultSet = model.getAdapter(ModelQueryAdapter.class).getResultSet(query);
		return () -> resultSet.size() * weight;
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		storeBuilder.getAdapter(ModelQueryBuilder.class).query(query);
	}
}
