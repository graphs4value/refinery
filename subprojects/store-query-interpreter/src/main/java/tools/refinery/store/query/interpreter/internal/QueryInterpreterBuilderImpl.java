/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal;

import org.eclipse.emf.ecore.EPackage;
import tools.refinery.interpreter.rete.recipes.RecipesPackage;
import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.rewriter.CompositeRewriter;
import tools.refinery.store.query.rewriter.DnfRewriter;
import tools.refinery.store.query.rewriter.DuplicateDnfRemover;
import tools.refinery.store.query.rewriter.InputParameterResolver;
import tools.refinery.store.query.interpreter.QueryInterpreterBuilder;
import tools.refinery.store.query.interpreter.internal.localsearch.FlatCostFunction;
import tools.refinery.store.query.interpreter.internal.matcher.RawPatternMatcher;
import tools.refinery.store.query.interpreter.internal.pquery.Dnf2PQuery;
import tools.refinery.interpreter.api.IQuerySpecification;
import tools.refinery.interpreter.api.InterpreterEngineOptions;
import tools.refinery.interpreter.localsearch.matcher.integration.LocalSearchGenericBackendFactory;
import tools.refinery.interpreter.localsearch.matcher.integration.LocalSearchHintOptions;
import tools.refinery.interpreter.matchers.backend.IQueryBackendFactory;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.rete.matcher.ReteBackendFactory;

import java.util.*;
import java.util.function.Function;

public class QueryInterpreterBuilderImpl extends AbstractModelAdapterBuilder<QueryInterpreterStoreAdapterImpl>
		implements QueryInterpreterBuilder {
	private InterpreterEngineOptions.Builder engineOptionsBuilder;
	private QueryEvaluationHint defaultHint = new QueryEvaluationHint(Map.of(
			// Use a cost function that ignores the initial (empty) model but allows higher arity input keys.
			LocalSearchHintOptions.PLANNER_COST_FUNCTION, new FlatCostFunction()
	), (IQueryBackendFactory) null);
	private final CompositeRewriter rewriter;
	private final Dnf2PQuery dnf2PQuery = new Dnf2PQuery();
	private final Set<AnyQuery> queries = new LinkedHashSet<>();

	public QueryInterpreterBuilderImpl() {
		EPackage.Registry.INSTANCE.put(RecipesPackage.eNS_URI, RecipesPackage.eINSTANCE);
		engineOptionsBuilder = new InterpreterEngineOptions.Builder()
				.withDefaultBackend(ReteBackendFactory.INSTANCE)
				.withDefaultCachingBackend(ReteBackendFactory.INSTANCE)
				.withDefaultSearchBackend(LocalSearchGenericBackendFactory.INSTANCE);
		rewriter = new CompositeRewriter();
		rewriter.addFirst(new DuplicateDnfRemover());
		rewriter.addFirst(new InputParameterResolver());
	}

	@Override
	public QueryInterpreterBuilder engineOptions(InterpreterEngineOptions engineOptions) {
		checkNotConfigured();
		engineOptionsBuilder = new InterpreterEngineOptions.Builder(engineOptions);
		return this;
	}

	@Override
	public QueryInterpreterBuilder defaultHint(QueryEvaluationHint queryEvaluationHint) {
		checkNotConfigured();
		defaultHint = defaultHint.overrideBy(queryEvaluationHint);
		return this;
	}

	@Override
	public QueryInterpreterBuilder backend(IQueryBackendFactory queryBackendFactory) {
		checkNotConfigured();
		engineOptionsBuilder.withDefaultBackend(queryBackendFactory);
		return this;
	}

	@Override
	public QueryInterpreterBuilder cachingBackend(IQueryBackendFactory queryBackendFactory) {
		checkNotConfigured();
		engineOptionsBuilder.withDefaultCachingBackend(queryBackendFactory);
		return this;
	}

	@Override
	public QueryInterpreterBuilder searchBackend(IQueryBackendFactory queryBackendFactory) {
		checkNotConfigured();
		engineOptionsBuilder.withDefaultSearchBackend(queryBackendFactory);
		return this;
	}

	@Override
	public QueryInterpreterBuilder queries(Collection<? extends AnyQuery> queries) {
		checkNotConfigured();
		this.queries.addAll(queries);
		return this;
	}

	@Override
	public QueryInterpreterBuilder query(AnyQuery query) {
		checkNotConfigured();
		queries.add(query);
		return this;
	}

	@Override
	public QueryInterpreterBuilder rewriter(DnfRewriter rewriter) {
		this.rewriter.addFirst(rewriter);
		return this;
	}

	@Override
	public QueryInterpreterBuilder computeHint(Function<Dnf, QueryEvaluationHint> computeHint) {
		checkNotConfigured();
		dnf2PQuery.setComputeHint(computeHint);
		return this;
	}

	@Override
	public QueryInterpreterStoreAdapterImpl doBuild(ModelStore store) {
		var canonicalQueryMap = new HashMap<AnyQuery, AnyQuery>();
		var querySpecifications = new LinkedHashMap<AnyQuery, IQuerySpecification<RawPatternMatcher>>();
		var vacuousQueries = new LinkedHashSet<AnyQuery>();
		for (var query : queries) {
			var canonicalQuery = rewriter.rewrite(query);
			canonicalQueryMap.put(query, canonicalQuery);
			var dnf = canonicalQuery.getDnf();
			var reduction = dnf.getReduction();
			switch (reduction) {
			case NOT_REDUCIBLE -> {
				var pQuery = dnf2PQuery.translate(dnf);
				querySpecifications.put(canonicalQuery, pQuery.build());
			}
			case ALWAYS_FALSE -> vacuousQueries.add(canonicalQuery);
			case ALWAYS_TRUE -> throw new IllegalArgumentException(
					"Query %s is relationally unsafe (it matches every tuple)".formatted(query.name()));
			default -> throw new IllegalArgumentException("Unknown reduction: " + reduction);
			}
		}

		validateSymbols(store);
		return new QueryInterpreterStoreAdapterImpl(store, buildEngineOptions(), dnf2PQuery.getSymbolViews(),
				Collections.unmodifiableMap(canonicalQueryMap), Collections.unmodifiableMap(querySpecifications),
				Collections.unmodifiableSet(vacuousQueries), store::checkCancelled);
	}

	private InterpreterEngineOptions buildEngineOptions() {
		// Workaround: manually override the default backend, because {@link ViatraQueryEngineOptions.Builder}
		// ignores all backend requirements except {@code SPECIFIC}.
		switch (defaultHint.getQueryBackendRequirementType()) {
		case SPECIFIC -> engineOptionsBuilder.withDefaultBackend(defaultHint.getQueryBackendFactory());
		case DEFAULT_CACHING -> engineOptionsBuilder.withDefaultBackend(
				engineOptionsBuilder.build().getDefaultCachingBackendFactory());
		case DEFAULT_SEARCH -> engineOptionsBuilder.withDefaultBackend(
				engineOptionsBuilder.build().getDefaultSearchBackendFactory());
		case UNSPECIFIED -> {
			// Nothing to do, leave the default backend unchanged.
		}
		}
		engineOptionsBuilder.withDefaultHint(defaultHint);
		return engineOptionsBuilder.build();
	}

	private void validateSymbols(ModelStore store) {
		var symbols = store.getSymbols();
		for (var symbolView : dnf2PQuery.getSymbolViews().keySet()) {
			var symbol = symbolView.getSymbol();
			if (!symbols.contains(symbol)) {
				throw new IllegalArgumentException("Cannot query view %s: symbol %s is not in the model"
						.formatted(symbolView, symbol));
			}
		}
	}
}
