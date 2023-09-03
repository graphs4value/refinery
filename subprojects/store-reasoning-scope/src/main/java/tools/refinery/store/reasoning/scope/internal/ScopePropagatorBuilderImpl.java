/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope.internal;

import com.google.ortools.Loader;
import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.scope.ScopePropagatorBuilder;
import tools.refinery.store.reasoning.scope.ScopePropagatorStoreAdapter;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.FiniteUpperCardinality;

import java.util.*;

import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.reasoning.ReasoningAdapter.EQUALS_SYMBOL;
import static tools.refinery.store.reasoning.ReasoningAdapter.EXISTS_SYMBOL;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

public class ScopePropagatorBuilderImpl extends AbstractModelAdapterBuilder<ScopePropagatorStoreAdapter>
		implements ScopePropagatorBuilder {
	private Symbol<CardinalityInterval> countSymbol = MultiObjectTranslator.COUNT_STORAGE;
	private final Map<PartialRelation, CardinalityInterval> scopes = new LinkedHashMap<>();
	private List<TypeScopePropagator.Factory> typeScopePropagatorFactories;

	@Override
	public ScopePropagatorBuilder countSymbol(Symbol<CardinalityInterval> countSymbol) {
		if (countSymbol.arity() != 1) {
			throw new IllegalArgumentException("Count symbol must have arty 1, got %s with arity %d instead"
					.formatted(countSymbol, countSymbol.arity()));
		}
		if (!countSymbol.valueType().equals(CardinalityInterval.class)) {
			throw new IllegalArgumentException("Count symbol must have CardinalityInterval values");
		}
		if (countSymbol.defaultValue() != null) {
			throw new IllegalArgumentException("Count symbol must default value null");
		}
		this.countSymbol = countSymbol;
		return this;
	}

	@Override
	public ScopePropagatorBuilder scope(PartialRelation type, CardinalityInterval interval) {
		if (type.arity() != 1) {
			throw new TranslationException(type, "Only types with arity 1 may have scopes, got %s with arity %d"
					.formatted(type, type.arity()));
		}
		var newValue = scopes.compute(type, (ignoredKey, oldValue) ->
				oldValue == null ? interval : oldValue.meet(interval));
		if (newValue.isEmpty()) {
			throw new TranslationException(type, "Unsatisfiable scope for type %s".formatted(type));
		}
		return this;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		var multiQuery = Query.of("MULTI", (builder, instance) -> builder
				.clause(
						may(EXISTS_SYMBOL.call(instance)),
						not(must(EXISTS_SYMBOL.call(instance)))
				)
				.clause(
						may(EXISTS_SYMBOL.call(instance)),
						not(must(EQUALS_SYMBOL.call(instance, instance)))
				)
		);
		typeScopePropagatorFactories = new ArrayList<>(scopes.size());
		for (var entry : scopes.entrySet()) {
			var type = entry.getKey();
			var bounds = entry.getValue();
			if (bounds.lowerBound() > 0) {
				var lowerFactory = new LowerTypeScopePropagator.Factory(multiQuery, type, bounds.lowerBound());
				typeScopePropagatorFactories.add(lowerFactory);
			}
			if (bounds.upperBound() instanceof FiniteUpperCardinality finiteUpperCardinality) {
				var upperFactory = new UpperTypeScopePropagator.Factory(multiQuery, type,
						finiteUpperCardinality.finiteUpperBound());
				typeScopePropagatorFactories.add(upperFactory);
			}
		}
		var queryBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		for (var factory : typeScopePropagatorFactories) {
			queryBuilder.queries(factory.getQueries());
		}
	}

	@Override
	protected ScopePropagatorStoreAdapter doBuild(ModelStore store) {
		Loader.loadNativeLibraries();
		return new ScopePropagatorStoreAdapterImpl(store, countSymbol, Collections.unmodifiableMap(scopes),
				Collections.unmodifiableList(typeScopePropagatorFactories));
	}
}
