/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.opposite;

import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.interpretation.PartialRelationRewriter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.refinement.RefinementBasedInitializer;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.TranslationException;

import java.util.List;
import java.util.Set;

public class OppositeRelationTranslator implements ModelStoreConfiguration, PartialRelationRewriter {
	private final PartialRelation linkType;
	private final PartialRelation opposite;

	public OppositeRelationTranslator(PartialRelation linkType, PartialRelation opposite) {
		if (linkType.arity() != 2) {
			throw new TranslationException(linkType,
					"Expected relation with opposite %s to have arity 2, got %d instead"
							.formatted(linkType, linkType.arity()));
		}
		if (opposite.arity() != 2) {
			throw new TranslationException(linkType,
					"Expected opposite %s of %s to have arity 2, got %d instead"
							.formatted(opposite, linkType, opposite.arity()));
		}
		this.linkType = linkType;
		this.opposite = opposite;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		storeBuilder.with(PartialRelationTranslator.of(linkType)
				.rewriter(this)
				.interpretation(OppositeInterpretation.of(opposite))
				.refiner(OppositeRefiner.of(opposite))
				.initializer(new RefinementBasedInitializer<>(linkType)));
	}

	@Override
	public List<Literal> rewriteLiteral(Set<Variable> positiveVariables, AbstractCallLiteral literal,
										Modality modality, Concreteness concreteness) {
		var arguments = literal.getArguments();
		var newArguments = List.of(arguments.get(1), arguments.get(0));
		var modalOpposite = new ModalConstraint(modality, concreteness, opposite);
		var oppositeLiteral = literal.withArguments(modalOpposite, newArguments);
		return List.of(oppositeLiteral);
	}
}
