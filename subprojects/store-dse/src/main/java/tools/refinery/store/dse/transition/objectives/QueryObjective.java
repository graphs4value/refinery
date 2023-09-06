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
		this.objectiveFunction = objectiveFunction;
	}

	@Override
	public ObjectiveCalculator createCalculator(Model model) {
		var resultSet = model.getAdapter(ModelQueryAdapter.class).getResultSet(objectiveFunction);
		return () -> {
			var cursor = resultSet.getAll();
			boolean hasElement = cursor.move();
			if(hasElement) {
				double result = cursor.getValue().doubleValue();
				if(cursor.move()) {
					throw new IllegalStateException("Query providing the objective function has multiple values!");
				}
				return result;
			} else {
				throw new IllegalStateException("Query providing the objective function has no values!");
			}
		};
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		Objective.super.configure(storeBuilder);
		storeBuilder.getAdapter(ModelQueryBuilder.class).query(objectiveFunction);
	}
}
