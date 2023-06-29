/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal;

import org.eclipse.viatra.query.runtime.api.AdvancedViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.GenericQueryGroup;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.internal.apiimpl.ViatraQueryEngineImpl;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackend;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendFactory;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelListener;
import tools.refinery.store.query.resultset.AnyResultSet;
import tools.refinery.store.query.resultset.EmptyResultSet;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.FunctionalQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.viatra.internal.matcher.FunctionalViatraMatcher;
import tools.refinery.store.query.viatra.internal.matcher.RawPatternMatcher;
import tools.refinery.store.query.viatra.internal.matcher.RelationalViatraMatcher;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ViatraModelQueryAdapterImpl implements ViatraModelQueryAdapter, ModelListener {
	private static final String DELAY_MESSAGE_DELIVERY_FIELD_NAME = "delayMessageDelivery";
	private static final MethodHandle SET_UPDATE_PROPAGATION_DELAYED_HANDLE;
	private static final String QUERY_BACKENDS_FIELD_NAME = "queryBackends";
	private static final MethodHandle GET_QUERY_BACKENDS_HANDLE;

	private final Model model;
	private final ViatraModelQueryStoreAdapterImpl storeAdapter;
	private final ViatraQueryEngineImpl queryEngine;
	private final Map<AnyQuery, AnyResultSet> resultSets;
	private boolean pendingChanges;

	static {
		try {
			var lookup = MethodHandles.privateLookupIn(ViatraQueryEngineImpl.class, MethodHandles.lookup());
			SET_UPDATE_PROPAGATION_DELAYED_HANDLE = lookup.findSetter(ViatraQueryEngineImpl.class,
					DELAY_MESSAGE_DELIVERY_FIELD_NAME, Boolean.TYPE);
			GET_QUERY_BACKENDS_HANDLE = lookup.findGetter(ViatraQueryEngineImpl.class, QUERY_BACKENDS_FIELD_NAME,
					Map.class);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new IllegalStateException("Cannot access private members of %s"
					.formatted(ViatraQueryEngineImpl.class.getName()), e);
		}
	}

	ViatraModelQueryAdapterImpl(Model model, ViatraModelQueryStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		var scope = new RelationalScope(this);
		queryEngine = (ViatraQueryEngineImpl) AdvancedViatraQueryEngine.createUnmanagedEngine(scope,
				storeAdapter.getEngineOptions());

		var querySpecifications = storeAdapter.getQuerySpecifications();
		GenericQueryGroup.of(
				Collections.<IQuerySpecification<?>>unmodifiableCollection(querySpecifications.values()).stream()
		).prepare(queryEngine);
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

		setUpdatePropagationDelayed(true);
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

	private void setUpdatePropagationDelayed(boolean value) {
		try {
			SET_UPDATE_PROPAGATION_DELAYED_HANDLE.invokeExact(queryEngine, value);
		} catch (Error e) {
			// Fatal JVM errors should not be wrapped.
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException("Cannot set %s".formatted(DELAY_MESSAGE_DELIVERY_FIELD_NAME), e);
		}
	}

	private Collection<IQueryBackend> getQueryBackends() {
		try {
			@SuppressWarnings("unchecked")
			var backendMap = (Map<IQueryBackendFactory, IQueryBackend>) GET_QUERY_BACKENDS_HANDLE.invokeExact(queryEngine);
			return backendMap.values();
		} catch (Error e) {
			// Fatal JVM errors should not be wrapped.
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException("Cannot get %s".formatted(QUERY_BACKENDS_FIELD_NAME), e);
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
		var resultSet = resultSets.get(query);
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
		if (!queryEngine.isUpdatePropagationDelayed()) {
			throw new IllegalStateException("Trying to flush changes while changes are already being flushed");
		}
		if (!pendingChanges) {
			return;
		}
		setUpdatePropagationDelayed(false);
		try {
			for (var queryBackend : getQueryBackends()) {
				queryBackend.flushUpdates();
			}
		} finally {
			setUpdatePropagationDelayed(true);
		}
		pendingChanges = false;
	}

	@Override
	public void afterRestore() {
		flushChanges();
	}
}
