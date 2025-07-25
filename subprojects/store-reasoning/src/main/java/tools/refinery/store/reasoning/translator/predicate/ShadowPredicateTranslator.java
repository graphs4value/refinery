/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.MissingInterpretation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;

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
				// Shadow predicates do not have to obey the refinement from the partial to the candidate model.
				.mergeCandidateWithPartial(false);
		if (!hasInterpretation) {
			translator.interpretation(MissingInterpretation::new);
		}
		storeBuilder.with(translator);
	}

}
