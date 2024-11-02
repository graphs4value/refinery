/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.ConcreteRelationRefiner;
import tools.refinery.store.reasoning.refinement.TypeConstraintRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

class DirectedCrossReferenceRefiner extends ConcreteRelationRefiner {
	private final PartialRelation sourceType;
	private final PartialRelation targetType;
	private final Set<PartialRelation> supersets;
	private final Set<PartialRelation> oppositeSupersets;
	private TypeConstraintRefiner typeConstraintRefiner;

	protected DirectedCrossReferenceRefiner(
			ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
			Symbol<TruthValue> concreteSymbol, DirectedCrossReferenceInfo info, RoundingMode roundingMode) {
		super(adapter, partialSymbol, concreteSymbol, roundingMode);
		this.sourceType = info.sourceType();
		this.targetType = info.targetType();
		this.supersets = info.supersets();
		this.oppositeSupersets = info.oppositeSupersets();
	}

	@Override
	public void afterCreate() {
		var adapter = getAdapter();
		typeConstraintRefiner = new TypeConstraintRefiner(adapter, sourceType, targetType, supersets,
				oppositeSupersets);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		if (!super.merge(key, value)) {
			return false;
		}
		if (value.must()) {
			return typeConstraintRefiner.merge(key);
		}
		return true;
	}

	@Override
	public void afterInitialize(ModelSeed modelSeed) {
		var linkType = getPartialSymbol();
		typeConstraintRefiner.mergeFromSeed(linkType, modelSeed);
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValue> concreteSymbol, DirectedCrossReferenceInfo info,
												  RoundingMode roundingMode) {
		return (adapter, partialSymbol) -> new DirectedCrossReferenceRefiner(adapter, partialSymbol, concreteSymbol,
				info, roundingMode);
	}
}
