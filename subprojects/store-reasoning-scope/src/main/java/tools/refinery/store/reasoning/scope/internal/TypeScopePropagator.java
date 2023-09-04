/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope.internal;

import com.google.ortools.linearsolver.MPConstraint;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.tuple.Tuple;

import java.util.Collection;

abstract class TypeScopePropagator {
	private final ScopePropagatorAdapterImpl adapter;
	private final ResultSet<Boolean> allNodes;
	private final ResultSet<Boolean> multiNodes;
	protected final MPConstraint constraint;

	protected TypeScopePropagator(ScopePropagatorAdapterImpl adapter, RelationalQuery allQuery,
							   RelationalQuery multiQuery) {
		this.adapter = adapter;
		var queryEngine = adapter.getModel().getAdapter(ModelQueryAdapter.class);
		allNodes = queryEngine.getResultSet(allQuery);
		multiNodes = queryEngine.getResultSet(multiQuery);
		constraint = adapter.makeConstraint();
		constraint.setBounds(0, Double.POSITIVE_INFINITY);
		var cursor = multiNodes.getAll();
		while (cursor.move()) {
			var variable = adapter.getVariable(cursor.getKey().get(0));
			constraint.setCoefficient(variable, 1);
		}
		allNodes.addListener(this::allChanged);
		multiNodes.addListener(this::multiChanged);
	}

	public abstract void updateBounds();

	protected int getSingleCount() {
		return allNodes.size() - multiNodes.size();
	}

	private void allChanged(Tuple ignoredKey, Boolean ignoredOldValue, Boolean ignoredNewValue) {
		adapter.markAsChanged();
	}

	private void multiChanged(Tuple key, Boolean ignoredOldValue, Boolean newValue) {
		var variable = adapter.getVariable(key.get(0));
		constraint.setCoefficient(variable, Boolean.TRUE.equals(newValue) ? 1 : 0);
		adapter.markAsChanged();
	}

	interface Factory {
		TypeScopePropagator createPropagator(ScopePropagatorAdapterImpl adapter);

		Collection<AnyQuery> getQueries();
	}
}