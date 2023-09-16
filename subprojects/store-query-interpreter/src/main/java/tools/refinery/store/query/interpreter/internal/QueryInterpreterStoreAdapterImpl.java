/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal;

import tools.refinery.interpreter.CancellationToken;
import tools.refinery.interpreter.api.IQuerySpecification;
import tools.refinery.interpreter.api.InterpreterEngineOptions;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.interpreter.QueryInterpreterStoreAdapter;
import tools.refinery.store.query.interpreter.internal.matcher.RawPatternMatcher;
import tools.refinery.store.query.view.AnySymbolView;

import java.util.*;

public class QueryInterpreterStoreAdapterImpl implements QueryInterpreterStoreAdapter {
	private final ModelStore store;
	private final InterpreterEngineOptions engineOptions;
	private final Map<AnySymbolView, IInputKey> inputKeys;
	private final Map<AnyQuery, AnyQuery> canonicalQueryMap;
	private final Map<AnyQuery, IQuerySpecification<RawPatternMatcher>> querySpecifications;
	private final Set<AnyQuery> vacuousQueries;
	private final Set<AnyQuery> allQueries;
	private final CancellationToken cancellationToken;

	QueryInterpreterStoreAdapterImpl(ModelStore store, InterpreterEngineOptions engineOptions,
									 Map<AnySymbolView, IInputKey> inputKeys,
									 Map<AnyQuery, AnyQuery> canonicalQueryMap,
									 Map<AnyQuery, IQuerySpecification<RawPatternMatcher>> querySpecifications,
									 Set<AnyQuery> vacuousQueries, CancellationToken cancellationToken) {
		this.store = store;
		this.engineOptions = engineOptions;
		this.inputKeys = inputKeys;
		this.canonicalQueryMap = canonicalQueryMap;
		this.querySpecifications = querySpecifications;
		this.vacuousQueries = vacuousQueries;
		this.cancellationToken = cancellationToken;
		var mutableAllQueries = new LinkedHashSet<AnyQuery>(querySpecifications.size() + vacuousQueries.size());
		mutableAllQueries.addAll(querySpecifications.keySet());
		mutableAllQueries.addAll(vacuousQueries);
		this.allQueries = Collections.unmodifiableSet(mutableAllQueries);
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	public Collection<AnySymbolView> getSymbolViews() {
		return inputKeys.keySet();
	}

	public Map<AnySymbolView, IInputKey> getInputKeys() {
		return inputKeys;
	}

	@Override
	public Collection<AnyQuery> getQueries() {
		return allQueries;
	}

	public CancellationToken getCancellationToken() {
		return cancellationToken;
	}

	@Override
	public <T> Query<T> getCanonicalQuery(Query<T> query) {
		// We know that canonical forms of queries do not change output types.
		@SuppressWarnings("unchecked")
		var canonicalQuery = (Query<T>) canonicalQueryMap.get(query);
		if (canonicalQuery == null) {
			throw new IllegalArgumentException("Unknown query: " + query);
		}
		return canonicalQuery;
	}

	Map<AnyQuery, IQuerySpecification<RawPatternMatcher>> getQuerySpecifications() {
		return querySpecifications;
	}

	Set<AnyQuery> getVacuousQueries() {
		return vacuousQueries;
	}

	@Override
	public InterpreterEngineOptions getEngineOptions() {
		return engineOptions;
	}

	@Override
	public QueryInterpreterAdapterImpl createModelAdapter(Model model) {
		return new QueryInterpreterAdapterImpl(model, this);
	}
}
