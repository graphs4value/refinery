package tools.refinery.store.query.viatra.internal;

import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngineOptions;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchHintOptions;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.rete.matcher.ReteBackendFactory;
import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.viatra.ViatraModelQueryBuilder;
import tools.refinery.store.query.viatra.internal.localsearch.FlatCostFunction;
import tools.refinery.store.query.viatra.internal.localsearch.RelationalLocalSearchBackendFactory;
import tools.refinery.store.query.viatra.internal.matcher.RawPatternMatcher;
import tools.refinery.store.query.viatra.internal.pquery.Dnf2PQuery;

import java.util.*;
import java.util.function.Function;

public class ViatraModelQueryBuilderImpl extends AbstractModelAdapterBuilder implements ViatraModelQueryBuilder {
	private ViatraQueryEngineOptions.Builder engineOptionsBuilder;
	private QueryEvaluationHint defaultHint = new QueryEvaluationHint(Map.of(
			// Use a cost function that ignores the initial (empty) model but allows higher arity input keys.
			LocalSearchHintOptions.PLANNER_COST_FUNCTION, new FlatCostFunction()
	), (IQueryBackendFactory) null);
	private final Dnf2PQuery dnf2PQuery = new Dnf2PQuery();
	private final Set<AnyQuery> vacuousQueries = new LinkedHashSet<>();
	private final Map<AnyQuery, IQuerySpecification<RawPatternMatcher>> querySpecifications = new LinkedHashMap<>();

	public ViatraModelQueryBuilderImpl(ModelStoreBuilder storeBuilder) {
		super(storeBuilder);
		engineOptionsBuilder = new ViatraQueryEngineOptions.Builder()
				.withDefaultBackend(ReteBackendFactory.INSTANCE)
				.withDefaultCachingBackend(ReteBackendFactory.INSTANCE)
				.withDefaultSearchBackend(RelationalLocalSearchBackendFactory.INSTANCE);
	}

	@Override
	public ViatraModelQueryBuilder engineOptions(ViatraQueryEngineOptions engineOptions) {
		engineOptionsBuilder = new ViatraQueryEngineOptions.Builder(engineOptions);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder defaultHint(QueryEvaluationHint queryEvaluationHint) {
		defaultHint = defaultHint.overrideBy(queryEvaluationHint);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder backend(IQueryBackendFactory queryBackendFactory) {
		engineOptionsBuilder.withDefaultBackend(queryBackendFactory);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder cachingBackend(IQueryBackendFactory queryBackendFactory) {
		engineOptionsBuilder.withDefaultCachingBackend(queryBackendFactory);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder searchBackend(IQueryBackendFactory queryBackendFactory) {
		engineOptionsBuilder.withDefaultSearchBackend(queryBackendFactory);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder query(AnyQuery query) {
		if (querySpecifications.containsKey(query) || vacuousQueries.contains(query)) {
			// Ignore duplicate queries.
			return this;
		}
		var dnf = query.getDnf();
		var reduction = dnf.getReduction();
		switch (reduction) {
		case NOT_REDUCIBLE -> {
			var pQuery = dnf2PQuery.translate(dnf);
			querySpecifications.put(query, pQuery.build());
		}
		case ALWAYS_FALSE -> vacuousQueries.add(query);
		case ALWAYS_TRUE -> throw new IllegalArgumentException(
				"Query %s is relationally unsafe (it matches every tuple)".formatted(query.name()));
		default -> throw new IllegalArgumentException("Unknown reduction: " + reduction);
		}
		return this;
	}

	@Override
	public ViatraModelQueryBuilder query(AnyQuery query, QueryEvaluationHint queryEvaluationHint) {
		hint(query.getDnf(), queryEvaluationHint);
		query(query);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder computeHint(Function<Dnf, QueryEvaluationHint> computeHint) {
		dnf2PQuery.setComputeHint(computeHint);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder hint(Dnf dnf, QueryEvaluationHint queryEvaluationHint) {
		dnf2PQuery.hint(dnf, queryEvaluationHint);
		return this;
	}

	@Override
	public ViatraModelQueryStoreAdapterImpl createStoreAdapter(ModelStore store) {
		validateSymbols(store);
		dnf2PQuery.assertNoUnusedHints();
		return new ViatraModelQueryStoreAdapterImpl(store, buildEngineOptions(), dnf2PQuery.getRelationViews(),
				Collections.unmodifiableMap(querySpecifications), Collections.unmodifiableSet(vacuousQueries));
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
		for (var relationView : dnf2PQuery.getRelationViews().keySet()) {
			var symbol = relationView.getSymbol();
			if (!symbols.contains(symbol)) {
				throw new IllegalArgumentException("Cannot query relation view %s: symbol %s is not in the model"
						.formatted(relationView, symbol));
			}
		}
	}
}
