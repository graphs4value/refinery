/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.ConcreteRelationRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;
import java.util.Objects;

class PredicateRefiner extends ConcreteRelationRefiner {
	private final List<PartialRelation> parameterTypes;
	private @Nullable PartialInterpretationRefiner<TruthValue, Boolean>[] parameterTypeRefiners;

	protected PredicateRefiner(
			ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
			Symbol<TruthValue> concreteSymbol, List<PartialRelation> parameterTypes, RoundingMode roundingMode) {
		super(adapter, partialSymbol, concreteSymbol, roundingMode);
		this.parameterTypes = parameterTypes;
	}

	@Override
	public void afterCreate() {
		int arity = parameterTypes.size();
		// Generic array creation.
		@SuppressWarnings("unchecked")
		PartialInterpretationRefiner<TruthValue, Boolean>[] array = new PartialInterpretationRefiner[arity];
		parameterTypeRefiners = array;
		var adapter = getAdapter();
		for (int i = 0; i < arity; i++) {
			var parameterType = parameterTypes.get(i);
			if (parameterType != null) {
				array[i] = adapter.getRefiner(parameterType);
			}
		}
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		var currentValue = get(key);
		var mergedValue = concretizationAwareMeet(currentValue, value);
		if (!Objects.equals(currentValue, mergedValue)) {
			put(key, mergedValue);
		}
		// Avoid cyclic propagation between parameter types by avoiding propagation after reaching a fixed point.
		if (mergedValue.must() && !currentValue.must()) {
			return refineParameters(key);
		}
		return true;
	}

	@Override
	public void afterInitialize(ModelSeed modelSeed) {
		var predicate = getPartialSymbol();
		var cursor = modelSeed.getCursor(predicate);
		while (cursor.move()) {
			var value = cursor.getValue();
			if (value.must()) {
				var key = cursor.getKey();
				if (!refineParameters(key)) {
					throw new IllegalArgumentException("Failed to merge parameter types of predicate %s for key %s"
							.formatted(predicate, key));
				}
			}
		}
	}

	private boolean refineParameters(Tuple key) {
		int arity = parameterTypeRefiners.length;
		for (int i = 0; i < arity; i++) {
			var refiner = parameterTypeRefiners[i];
			if (refiner != null && !refiner.merge(Tuple.of(key.get(i)), TruthValue.TRUE)) {
				return false;
			}
		}
		return true;
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValue> concreteSymbol,
												  List<PartialRelation> parameterTypes, RoundingMode roundingMode) {
		return (adapter, partialSymbol) -> new PredicateRefiner(adapter, partialSymbol, concreteSymbol,
				parameterTypes, roundingMode);
	}
}
