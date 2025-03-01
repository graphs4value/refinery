/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal;

import tools.refinery.interpreter.api.AdvancedInterpreterEngine;
import tools.refinery.interpreter.api.GenericQueryGroup;
import tools.refinery.interpreter.api.IQuerySpecification;
import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.store.query.interpreter.internal.matcher.InterpretedFunctionalMatcher;
import tools.refinery.store.query.interpreter.internal.matcher.InterpretedRelationalMatcher;
import tools.refinery.store.query.interpreter.internal.matcher.RawPatternMatcher;
import tools.refinery.store.query.resultset.AlwaysTrueResultSet;
import tools.refinery.store.query.resultset.AnyResultSet;
import tools.refinery.store.query.resultset.EmptyResultSet;
import tools.refinery.store.query.resultset.ResultSet;

import java.util.*;

class ValidatedQueries {
	private final Map<AnyQuery, AnyQuery> canonicalQueryMap;
	private final Map<AnyQuery, IQuerySpecification<RawPatternMatcher>> querySpecifications;
	private final Set<AnyQuery> vacuousQueries;
	private final Set<RelationalQuery> alwaysTrueQueries;
	private final Set<AnyQuery> allQueries;

	public ValidatedQueries(Map<AnyQuery, AnyQuery> canonicalQueryMap,
							Map<AnyQuery, IQuerySpecification<RawPatternMatcher>> querySpecifications,
							Set<AnyQuery> vacuousQueries, Set<RelationalQuery> alwaysTrueQueries) {
		this.canonicalQueryMap = Collections.unmodifiableMap(canonicalQueryMap);
		this.querySpecifications = Collections.unmodifiableMap(querySpecifications);
		this.vacuousQueries = Collections.unmodifiableSet(vacuousQueries);
		this.alwaysTrueQueries = Collections.unmodifiableSet(alwaysTrueQueries);
		var mutableAllQueries = LinkedHashSet.<AnyQuery>newLinkedHashSet(
				querySpecifications.size() + vacuousQueries.size() + alwaysTrueQueries.size());
		mutableAllQueries.addAll(querySpecifications.keySet());
		mutableAllQueries.addAll(vacuousQueries);
		mutableAllQueries.addAll(alwaysTrueQueries);
		this.allQueries = Collections.unmodifiableSet(mutableAllQueries);
	}

	public Map<AnyQuery, AnyQuery> getCanonicalQueryMap() {
		return canonicalQueryMap;
	}

	public Set<AnyQuery> getAllQueries() {
		return allQueries;
	}

	public Map<AnyQuery, AnyResultSet> instantiate(QueryInterpreterAdapterImpl adapter,
												   AdvancedInterpreterEngine queryEngine) {
		GenericQueryGroup.of(
				Collections.<IQuerySpecification<?>>unmodifiableCollection(querySpecifications.values()).stream()
		).prepare(queryEngine);
		queryEngine.flushChanges();
		var resultSets = LinkedHashMap.<AnyQuery, AnyResultSet>newLinkedHashMap(allQueries.size());
		for (var entry : querySpecifications.entrySet()) {
			var rawPatternMatcher = queryEngine.getMatcher(entry.getValue());
			var query = entry.getKey();
			resultSets.put(query, createResultSet(adapter, (Query<?>) query, rawPatternMatcher));
		}
		for (var vacuousQuery : vacuousQueries) {
			resultSets.put(vacuousQuery, new EmptyResultSet<>(adapter, (Query<?>) vacuousQuery));
		}
		for (var alwaysTrueQuery : alwaysTrueQueries) {
			resultSets.put(alwaysTrueQuery, new AlwaysTrueResultSet(adapter, alwaysTrueQuery));
		}
		return resultSets;
	}

	private <T> ResultSet<T> createResultSet(QueryInterpreterAdapterImpl adapter, Query<T> query,
											 RawPatternMatcher matcher) {
		return switch (query) {
			case RelationalQuery relationalQuery -> {
				@SuppressWarnings("unchecked")
				var resultSet = (ResultSet<T>) new InterpretedRelationalMatcher(adapter, relationalQuery, matcher);
				yield resultSet;
			}
			case FunctionalQuery<T> functionalQuery ->
					new InterpretedFunctionalMatcher<>(adapter, functionalQuery, matcher);
		};
	}
}
