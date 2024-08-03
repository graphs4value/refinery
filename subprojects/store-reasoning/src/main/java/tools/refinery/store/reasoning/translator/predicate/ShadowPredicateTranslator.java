/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.interpretation.AbstractPartialInterpretation;
import tools.refinery.store.reasoning.interpretation.QueryBasedComputedRewriter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.tuple.Tuple;

public class ShadowPredicateTranslator implements ModelStoreConfiguration {
	private final PartialRelation relation;
	private final RelationalQuery query;
	private final boolean hasInterpretation;

	public ShadowPredicateTranslator(PartialRelation relation, RelationalQuery query, boolean hasInterpretation) {
		this.relation = relation;
		this.query = query;
		this.hasInterpretation = hasInterpretation;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);
		var may = reasoningBuilder.lift(Modality.MAY, Concreteness.PARTIAL, query);
		var must = reasoningBuilder.lift(Modality.MAY, Concreteness.PARTIAL, query);
		// Do not let {@link PartialRelationTranslator} merge the partial queries into the candidate ones.
		var candidateMay = reasoningBuilder.lift(Modality.MAY, Concreteness.PARTIAL, query);
		var candidateMust = reasoningBuilder.lift(Modality.MAY, Concreteness.PARTIAL, query);
		var translator = PartialRelationTranslator.of(relation)
				.rewriter(new QueryBasedComputedRewriter(may, must, candidateMay, candidateMust, query));
		if (!hasInterpretation) {
			translator.interpretation(MissingInterpretation::new);
		}
		storeBuilder.with(translator);
	}

	private static class MissingInterpretation extends AbstractPartialInterpretation<TruthValue, Boolean> {
		public MissingInterpretation(ReasoningAdapter adapter, Concreteness concreteness,
									 PartialSymbol<TruthValue, Boolean> partialSymbol) {
			super(adapter, concreteness, partialSymbol);
		}

		@Override
		public TruthValue get(Tuple key) {
			return fail();
		}

		@Override
		public Cursor<Tuple, TruthValue> getAll() {
			return fail();
		}

		private <T> T fail() {
			throw new UnsupportedOperationException("No interpretation for shadow predicate: " + getPartialSymbol());
		}
	}
}
