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
import tools.refinery.store.query.dnf.FunctionalQuery;

public class QueryObjective implements Objective {
	protected final FunctionalQuery<? extends Number> objectiveFunction;

	public QueryObjective(FunctionalQuery<? extends Number> objectiveFunction) {
		if (objectiveFunction.arity() != 0) {
			throw new IllegalArgumentException("Objective functions must have 0 parameters, got %d instead"
					.formatted(objectiveFunction.arity()));
		}
		this.objectiveFunction = objectiveFunction;
	}

	@Override
	public ObjectiveCalculator createCalculator(Model model) {
		var resultSet = model.getAdapter(ModelQueryAdapter.class).getResultSet(objectiveFunction);
		return () -> {
			var cursor = resultSet.getAll();
			if (!cursor.move()) {
				return 0;
			}
			return Math.max(cursor.getValue().doubleValue(), 0);
		};
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		storeBuilder.getAdapter(ModelQueryBuilder.class).query(objectiveFunction);
	}
}
