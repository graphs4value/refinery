/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelListener;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.FunctionalQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.resultset.AnyResultSet;
import tools.refinery.store.query.resultset.EmptyResultSet;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.viatra.internal.matcher.FunctionalViatraMatcher;
import tools.refinery.store.query.viatra.internal.matcher.RawPatternMatcher;
import tools.refinery.store.query.viatra.internal.matcher.RelationalViatraMatcher;
import tools.refinery.viatra.runtime.api.AdvancedViatraQueryEngine;
import tools.refinery.viatra.runtime.api.GenericQueryGroup;
import tools.refinery.viatra.runtime.api.IQuerySpecification;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ViatraModelQueryAdapterImpl implements ViatraModelQueryAdapter, ModelListener {
	private final Model model;
	private final ViatraModelQueryStoreAdapterImpl storeAdapter;
	private final AdvancedViatraQueryEngine queryEngine;
	private final Map<AnyQuery, AnyResultSet> resultSets;
	private boolean pendingChanges;

	ViatraModelQueryAdapterImpl(Model model, ViatraModelQueryStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		var scope = new RelationalScope(this);
		queryEngine = AdvancedViatraQueryEngine.createUnmanagedEngine(scope,
				storeAdapter.getEngineOptions());

		var querySpecifications = storeAdapter.getQuerySpecifications();
		GenericQueryGroup.of(
				Collections.<IQuerySpecification<?>>unmodifiableCollection(querySpecifications.values()).stream()
		).prepare(queryEngine);
		queryEngine.flushChanges();
		var vacuousQueries = storeAdapter.getVacuousQueries();
		resultSets = new LinkedHashMap<>(querySpecifications.size() + vacuousQueries.size());
		for (var entry : querySpecifications.entrySet()) {
			var rawPatternMatcher = queryEngine.getMatcher(entry.getValue());
			var query = entry.getKey();
			resultSets.put(query, createResultSet((Query<?>) query, rawPatternMatcher));
		}
		for (var vacuousQuery : vacuousQueries) {
			resultSets.put(vacuousQuery, new EmptyResultSet<>(this, (Query<?>) vacuousQuery));
		}

		model.addListener(this);
	}

	private <T> ResultSet<T> createResultSet(Query<T> query, RawPatternMatcher matcher) {
		if (query instanceof RelationalQuery relationalQuery) {
			@SuppressWarnings("unchecked")
			var resultSet = (ResultSet<T>) new RelationalViatraMatcher(this, relationalQuery, matcher);
			return resultSet;
		} else if (query instanceof FunctionalQuery<T> functionalQuery) {
			return new FunctionalViatraMatcher<>(this, functionalQuery, matcher);
		} else {
			throw new IllegalArgumentException("Unknown query: " + query);
		}
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public ViatraModelQueryStoreAdapterImpl getStoreAdapter() {
		return storeAdapter;
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
}
