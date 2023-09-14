/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.proxy;

import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.interpretation.PartialRelationRewriter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;

import java.util.List;
import java.util.Set;

public class PartialRelationTranslatorProxy implements ModelStoreConfiguration, PartialRelationRewriter {
	private final PartialRelation partialRelation;
	private final PartialRelation targetRelation;
	private final boolean mutable;

	public PartialRelationTranslatorProxy(PartialRelation partialRelation, PartialRelation targetRelation,
										  boolean mutable) {
		this.partialRelation = partialRelation;
		this.targetRelation = targetRelation;
		this.mutable = mutable;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var translator = PartialRelationTranslator.of(partialRelation)
				.interpretation(((adapter, concreteness, partialSymbol) ->
						adapter.getPartialInterpretation(concreteness, targetRelation)))
				.rewriter(this);
		if (mutable) {
			translator.refiner((adapter, partialSymbol) -> adapter.getRefiner(targetRelation));
		}
		storeBuilder.with(translator);
	}

	@Override
	public List<Literal> rewriteLiteral(Set<Variable> positiveVariables, AbstractCallLiteral literal,
										Modality modality, Concreteness concreteness) {
		return List.of(literal.withTarget(ModalConstraint.of(modality, concreteness, targetRelation)));
	}
}
