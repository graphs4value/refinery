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
import tools.refinery.store.reasoning.interpretation.AbstractPartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
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
		var translator = PartialRelationTranslator.of(relation)
				.query(query)
				// Shadow predicates ddo not have to obey the refinement from the partial to the candidate model.
				.mergeCandidateWithPartial(false);
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
