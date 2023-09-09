/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import com.google.ortools.Loader;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.FiniteUpperCardinality;

import java.util.*;

public class ScopePropagator implements ModelStoreConfiguration {
	private final Symbol<CardinalityInterval> countSymbol;
	private final Map<PartialRelation, CardinalityInterval> scopes = new LinkedHashMap<>();
	private final List<TypeScopePropagator.Factory> typeScopePropagatorFactories = new ArrayList<>();

	public ScopePropagator() {
		this(MultiObjectTranslator.COUNT_STORAGE);
	}

	public ScopePropagator(Symbol<CardinalityInterval> countSymbol) {
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
	}

	public ScopePropagator scope(PartialRelation type, CardinalityInterval interval) {
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

	public ScopePropagator scopes(Map<PartialRelation, CardinalityInterval> scopes) {
		return scopes(scopes.entrySet());
	}

	public ScopePropagator scopes(Collection<Map.Entry<PartialRelation, CardinalityInterval>> scopes) {
		for (var entry : scopes) {
			scope(entry.getKey(), entry.getValue());
		}
		return this;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		createTypeScopePropagatorFactories();
		Loader.loadNativeLibraries();
		for (var factory : typeScopePropagatorFactories) {
			factory.configure(storeBuilder);
		}
		storeBuilder.getAdapter(PropagationBuilder.class)
				.propagator(model -> new BoundScopePropagator(model, this));
	}

	private void createTypeScopePropagatorFactories() {
		for (var entry : scopes.entrySet()) {
			var type = entry.getKey();
			var bounds = entry.getValue();
			if (bounds.lowerBound() > 0) {
				var lowerFactory = new LowerTypeScopePropagator.Factory(type, bounds.lowerBound());
				typeScopePropagatorFactories.add(lowerFactory);
			}
			if (bounds.upperBound() instanceof FiniteUpperCardinality finiteUpperCardinality) {
				var upperFactory = new UpperTypeScopePropagator.Factory(type,
						finiteUpperCardinality.finiteUpperBound());
				typeScopePropagatorFactories.add(upperFactory);
			}
		}
	}

	Symbol<CardinalityInterval> getCountSymbol() {
		return countSymbol;
	}

	List<TypeScopePropagator.Factory> getTypeScopePropagatorFactories() {
		return typeScopePropagatorFactories;
	}
}
