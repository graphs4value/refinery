package tools.refinery.store.query.viatra.internal;

import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngineOptions;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchGenericBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.rete.matcher.ReteBackendFactory;
import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.viatra.ViatraModelQueryBuilder;
import tools.refinery.store.query.viatra.internal.pquery.Dnf2PQuery;
import tools.refinery.store.query.viatra.internal.pquery.RawPatternMatcher;

import java.util.*;
import java.util.function.Function;

public class ViatraModelQueryBuilderImpl extends AbstractModelAdapterBuilder implements ViatraModelQueryBuilder {
	private ViatraQueryEngineOptions.Builder engineOptionsBuilder;
	private final Dnf2PQuery dnf2PQuery = new Dnf2PQuery();
	private final Set<Dnf> vacuousQueries = new LinkedHashSet<>();
	private final Map<Dnf, IQuerySpecification<RawPatternMatcher>> querySpecifications = new LinkedHashMap<>();

	public ViatraModelQueryBuilderImpl(ModelStoreBuilder storeBuilder) {
		super(storeBuilder);
		engineOptionsBuilder = new ViatraQueryEngineOptions.Builder()
				.withDefaultBackend(ReteBackendFactory.INSTANCE)
				.withDefaultCachingBackend(ReteBackendFactory.INSTANCE)
				.withDefaultSearchBackend(LocalSearchGenericBackendFactory.INSTANCE);
	}

	@Override
	public ViatraModelQueryBuilder engineOptions(ViatraQueryEngineOptions engineOptions) {
		engineOptionsBuilder = new ViatraQueryEngineOptions.Builder(engineOptions);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder defaultHint(QueryEvaluationHint queryEvaluationHint) {
		engineOptionsBuilder.withDefaultHint(queryEvaluationHint);
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
	public ViatraModelQueryBuilder query(Dnf query) {
		if (querySpecifications.containsKey(query) || vacuousQueries.contains(query)) {
			// Ignore duplicate queries.
			return this;
		}
		var reduction = query.getReduction();
		switch (reduction) {
		case NOT_REDUCIBLE -> {
			var pQuery = dnf2PQuery.translate(query);
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
	public ViatraModelQueryBuilder query(Dnf query, QueryEvaluationHint queryEvaluationHint) {
		query(query);
		hint(query, queryEvaluationHint);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder computeHint(Function<Dnf, QueryEvaluationHint> computeHint) {
		dnf2PQuery.setComputeHint(computeHint);
		return this;
	}

	@Override
	public ViatraModelQueryBuilder hint(Dnf dnf, QueryEvaluationHint queryEvaluationHint) {
		var pQuery = dnf2PQuery.getAlreadyTranslated(dnf);
		if (pQuery == null) {
			if (vacuousQueries.contains(dnf)) {
				// Ignore hits for queries that will never be executed by the query engine.
				return this;
			}
			throw new IllegalArgumentException(
					"Cannot specify hint for %s, because it was not added to the query engine".formatted(dnf.name()));
		}
		pQuery.setEvaluationHints(pQuery.getEvaluationHints().overrideBy(queryEvaluationHint));
		return this;
	}

	@Override
	public ViatraModelQueryStoreAdapterImpl createStoreAdapter(ModelStore store) {
		validateSymbols(store);
		return new ViatraModelQueryStoreAdapterImpl(store, engineOptionsBuilder.build(), dnf2PQuery.getRelationViews(),
				Collections.unmodifiableMap(querySpecifications), Collections.unmodifiableSet(vacuousQueries));
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
