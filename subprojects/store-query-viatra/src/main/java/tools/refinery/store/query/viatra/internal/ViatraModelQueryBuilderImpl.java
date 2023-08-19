/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.rewriter.CompositeRewriter;
import tools.refinery.store.query.rewriter.DnfRewriter;
import tools.refinery.store.query.rewriter.DuplicateDnfRemover;
import tools.refinery.store.query.rewriter.InputParameterResolver;
import tools.refinery.store.query.viatra.ViatraModelQueryBuilder;
import tools.refinery.store.query.viatra.internal.localsearch.FlatCostFunction;
import tools.refinery.store.query.viatra.internal.matcher.RawPatternMatcher;
import tools.refinery.store.query.viatra.internal.pquery.Dnf2PQuery;
import tools.refinery.viatra.runtime.CancellationToken;
import tools.refinery.viatra.runtime.api.IQuerySpecification;
import tools.refinery.viatra.runtime.api.ViatraQueryEngineOptions;
import tools.refinery.viatra.runtime.localsearch.matcher.integration.LocalSearchGenericBackendFactory;
import tools.refinery.viatra.runtime.localsearch.matcher.integration.LocalSearchHintOptions;
import tools.refinery.viatra.runtime.matchers.backend.IQueryBackendFactory;
import tools.refinery.viatra.runtime.matchers.backend.QueryEvaluationHint;
import tools.refinery.viatra.runtime.rete.matcher.ReteBackendFactory;

import java.util.*;
import java.util.function.Function;

public class ViatraModelQueryBuilderImpl extends AbstractModelAdapterBuilder<ViatraModelQueryStoreAdapterImpl>
		implements ViatraModelQueryBuilder {
	private ViatraQueryEngineOptions.Builder engineOptionsBuilder;
	private QueryEvaluationHint defaultHint = new QueryEvaluationHint(Map.of(
			// Use a cost function that ignores the initial (empty) model but allows higher arity input keys.
			LocalSearchHintOptions.PLANNER_COST_FUNCTION, new FlatCostFunction()
	), (IQueryBackendFactory) null);
	private CancellationToken cancellationToken = CancellationToken.NONE;
	private final CompositeRewriter rewriter;
	private final Dnf2PQuery dnf2PQuery = new Dnf2PQuery();
	private final Set<AnyQuery> queries = new LinkedHashSet<>();

	public ViatraModelQueryBuilderImpl() {
		engineOptionsBuilder = new ViatraQueryEngineOptions.Builder()
				.withDefaultBackend(ReteBackendFactory.INSTANCE)
				.withDefaultCachingBackend(ReteBackendFactory.INSTANCE)
				.withDefaultSearchBackend(LocalSearchGenericBackendFactory.INSTANCE);
		rewriter = new CompositeRewriter();
		rewriter.addFirst(new DuplicateDnfRemover());
		rewriter.addFirst(new InputParameterResolver());
	}

	@Override
	public ViatraModelQueryBuilder engineOptions(ViatraQueryEngineOptions engineOptions) {
		checkNotConfigured();
		engineOptionsBuilder = new ViatraQueryEngineOptions.Builder(engineOptions);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder defaultHint(QueryEvaluationHint queryEvaluationHint) {
		checkNotConfigured();
		defaultHint = defaultHint.overrideBy(queryEvaluationHint);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder backend(IQueryBackendFactory queryBackendFactory) {
		checkNotConfigured();
		engineOptionsBuilder.withDefaultBackend(queryBackendFactory);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder cachingBackend(IQueryBackendFactory queryBackendFactory) {
		checkNotConfigured();
		engineOptionsBuilder.withDefaultCachingBackend(queryBackendFactory);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder searchBackend(IQueryBackendFactory queryBackendFactory) {
		checkNotConfigured();
		engineOptionsBuilder.withDefaultSearchBackend(queryBackendFactory);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder cancellationToken(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
		return this;
	}

	@Override
	public ViatraModelQueryBuilder queries(Collection<? extends AnyQuery> queries) {
		checkNotConfigured();
		this.queries.addAll(queries);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder query(AnyQuery query) {
		checkNotConfigured();
		queries.add(query);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder rewriter(DnfRewriter rewriter) {
		this.rewriter.addFirst(rewriter);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder computeHint(Function<Dnf, QueryEvaluationHint> computeHint) {
		checkNotConfigured();
		dnf2PQuery.setComputeHint(computeHint);
		return this;
	}

	@Override
	public ViatraModelQueryStoreAdapterImpl doBuild(ModelStore store) {
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
		return new ViatraModelQueryStoreAdapterImpl(store, buildEngineOptions(), dnf2PQuery.getSymbolViews(),
				Collections.unmodifiableMap(canonicalQueryMap), Collections.unmodifiableMap(querySpecifications),
				Collections.unmodifiableSet(vacuousQueries), cancellationToken);
	}

	private ViatraQueryEngineOptions buildEngineOptions() {
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
