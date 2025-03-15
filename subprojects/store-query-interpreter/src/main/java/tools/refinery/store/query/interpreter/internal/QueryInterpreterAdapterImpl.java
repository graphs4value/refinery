/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal;

import tools.refinery.interpreter.CancellationToken;
import tools.refinery.interpreter.api.AdvancedInterpreterEngine;
import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelListener;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.resultset.AnyResultSet;
import tools.refinery.store.query.resultset.ResultSet;

import java.util.Map;

public class QueryInterpreterAdapterImpl implements QueryInterpreterAdapter, ModelListener {
	private final Model model;
	private final QueryInterpreterStoreAdapterImpl storeAdapter;
	private final AdvancedInterpreterEngine queryEngine;
	private final Map<AnyQuery, AnyResultSet> resultSets;
	private boolean pendingChanges;

	QueryInterpreterAdapterImpl(Model model, QueryInterpreterStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		var scope = new RelationalScope(this);
		queryEngine = AdvancedInterpreterEngine.createUnmanagedEngine(scope,
				storeAdapter.getEngineOptions());
		resultSets = storeAdapter.getValidatedQueries().instantiate(this, queryEngine);
		model.addListener(this);
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public QueryInterpreterStoreAdapterImpl getStoreAdapter() {
		return storeAdapter;
	}

	public CancellationToken getCancellationToken() {
		return storeAdapter.getCancellationToken();
	}

	@Override
	public <T> ResultSet<T> getResultSet(Query<T> query) {
		var canonicalQuery = storeAdapter.getCanonicalQuery(query);
		var resultSet = resultSets.get(canonicalQuery);
		if (resultSet == null) {
			throw new IllegalArgumentException("No matcher for query %s in model".formatted(query.name()));
		}
		@SuppressWarnings("unchecked")
		var typedResultSet = (ResultSet<T>) resultSet;
		return typedResultSet;
	}

	@Override
	public boolean hasPendingChanges() {
		return pendingChanges;
	}

	public void markAsPending() {
		if (!pendingChanges) {
			pendingChanges = true;
		}
	}

	@Override
	public void flushChanges() {
		queryEngine.flushChanges();
		pendingChanges = false;
	}

	@Override
	public void afterRestore() {
		flushChanges();
	}

	@Override
	public void beforeClose() {
		queryEngine.dispose();
	}
}
