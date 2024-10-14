/*
 * SPDX-FileCopyrightText: 20232-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiplicity;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.uppercardinality.FiniteUpperCardinality;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;
import tools.refinery.logic.term.uppercardinality.UpperCardinalityTerms;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.*;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.TranslationException;

import java.util.List;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.int_.IntTerms.*;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;

public class InvalidMultiplicityErrorTranslator implements ModelStoreConfiguration {
	private final PartialRelation nodeType;
	private final PartialRelation linkType;
	private final boolean inverse;
	private final Multiplicity multiplicity;

	public InvalidMultiplicityErrorTranslator(PartialRelation nodeType, PartialRelation linkType,
											  boolean inverse, Multiplicity multiplicity) {
		if (nodeType.arity() != 1) {
			throw new TranslationException(linkType, "Node type must be of arity 1, got %s with arity %d instead"
					.formatted(nodeType, nodeType.arity()));
		}
		if (linkType.arity() != 2) {
			throw new TranslationException(linkType, "Link type must be of arity 2, got %s with arity %d instead"
					.formatted(linkType, linkType.arity()));
		}
		this.nodeType = nodeType;
		this.linkType = linkType;
		this.inverse = inverse;
		this.multiplicity = multiplicity;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		if (!(multiplicity instanceof ConstrainedMultiplicity(var cardinalityInterval, var errorSymbol))) {
			return;
		}

		var name = errorSymbol.name();
		var node = Variable.of("node");
		var other = Variable.of("other");
		List<Variable> arguments = inverse ? List.of(other, node) : List.of(node, other);
		var mustBuilder = Query.builder(DnfLifter.decorateName(name, Modality.MUST, Concreteness.PARTIAL))
				.parameter(node);
		var candidateMayBuilder = Query.builder(DnfLifter.decorateName(name, Modality.MAY, Concreteness.PARTIAL))
				.parameter(node);
		var candidateMustBuilder = Query.builder(DnfLifter.decorateName(name, Modality.MUST, Concreteness.PARTIAL))
				.parameter(node);
		var missingOutput = Variable.of("missing", Integer.class);
		var missingBuilder = Query.builder(name + "#missingMultiplicity").parameter(node).output(missingOutput);

		int lowerBound = cardinalityInterval.lowerBound();
		if (lowerBound > 0) {
			var lowerBoundCardinality = UpperCardinalities.atMost(lowerBound);
			mustBuilder.clause(UpperCardinality.class, existingContents -> List.of(
					must(nodeType.call(node)),
					new CountUpperBoundLiteral(existingContents, linkType, arguments),
					check(UpperCardinalityTerms.less(existingContents,
							UpperCardinalityTerms.constant(lowerBoundCardinality)))
			));
			candidateMayBuilder.clause(Integer.class, existingContents -> List.of(
					candidateMust(nodeType.call(node)),
					new CountCandidateLowerBoundLiteral(existingContents, linkType, arguments),
					check(less(existingContents, constant(lowerBound)))
			));
			candidateMustBuilder.clause(Integer.class, existingContents -> List.of(
					candidateMust(nodeType.call(node)),
					new CountCandidateUpperBoundLiteral(existingContents, linkType, arguments),
					check(less(existingContents, constant(lowerBound)))
			));
			missingBuilder.clause(Integer.class, existingContents -> List.of(
					candidateMust(nodeType.call(node)),
					candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(node)),
					new CountCandidateLowerBoundLiteral(existingContents, linkType, arguments),
					missingOutput.assign(sub(constant(lowerBound), existingContents)),
					check(greater(missingOutput, constant(0)))
			));
		}

		if (cardinalityInterval.upperBound() instanceof FiniteUpperCardinality(int upperBound)) {
			mustBuilder.clause(Integer.class, existingContents -> List.of(
					must(nodeType.call(node)),
					new CountLowerBoundLiteral(existingContents, linkType, arguments),
					check(greater(existingContents, constant(upperBound)))
			));
			candidateMayBuilder.clause(Integer.class, existingContents -> List.of(
					candidateMust(nodeType.call(node)),
					new CountCandidateUpperBoundLiteral(existingContents, linkType, arguments),
					check(greater(existingContents, constant(upperBound)))
			));
			candidateMustBuilder.clause(Integer.class, existingContents -> List.of(
					candidateMust(nodeType.call(node)),
					new CountCandidateLowerBoundLiteral(existingContents, linkType, arguments),
					check(greater(existingContents, constant(upperBound)))
			));
			missingBuilder.clause(Integer.class, existingContents -> List.of(
					candidateMust(nodeType.call(node)),
					candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(node)),
					new CountCandidateUpperBoundLiteral(existingContents, linkType, arguments),
					missingOutput.assign(sub(existingContents, constant(upperBound))),
					check(greater(missingOutput, constant(0)))
			));
		}

		var objective = Query.of(name + "#objective", Integer.class, (builder, output) -> builder.clause(
				output.assign(missingBuilder.build().aggregate(INT_SUM, Variable.of()))
		));

		storeBuilder.with(PartialRelationTranslator.of(errorSymbol)
				.mayNever()
				.must(mustBuilder.build())
				.candidateMay(candidateMayBuilder.build())
				.candidateMust(candidateMustBuilder.build())
				.objective(Objectives.value(objective)));

		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(this::configureLowerMultiplicityPropagator);
	}

	private void configureLowerMultiplicityPropagator(PropagationBuilder propagationBuilder) {
		if (!(multiplicity instanceof ConstrainedMultiplicity constrainedMultiplicity)) {
			return;
		}
		int lowerBound = constrainedMultiplicity.multiplicity().lowerBound();
		if (lowerBound < 1) {
			return;
		}
		var name = linkType.name();
		var lowerBoundCardinality = UpperCardinalities.atMost(lowerBound);
		var lowerSuffix = inverse ? "#lowerTarget" : "#lowerSource";
		propagationBuilder.rule(Rule.of(name + lowerSuffix, (builder, p1, p2) -> builder
				.clause(UpperCardinality.class, upperBound -> List.of(
						must(nodeType.call(inverse ? p2 : p1)),
						new CountUpperBoundLiteral(upperBound, linkType,
								inverse ? List.of(Variable.of(), p2) : List.of(p1, Variable.of())),
						check(UpperCardinalityTerms.lessEq(upperBound,
								UpperCardinalityTerms.constant(lowerBoundCardinality))),
						may(linkType.call(p1, p2)),
						not(must(linkType.call(p1, p2)))
				))
				.action(
						add(linkType, p1, p2)
				)
		));
		var missingSuffix = inverse ? "#missingSource" : "#missingTarget";
		propagationBuilder.rule(Rule.of(name + missingSuffix, (builder, p1) -> builder
				.clause(UpperCardinality.class, upperBound -> List.of(
						may(nodeType.call(p1)),
						// Violation of monotonicity: stop the propagation of inconsistencies, since the
						// {@code invalidMultiplicity} pattern will already mark the model as invalid.
						not(must(nodeType.call(p1))),
						new CountUpperBoundLiteral(upperBound, linkType,
								inverse ? List.of(Variable.of(), p1) : List.of(p1, Variable.of())),
						check(UpperCardinalityTerms.less(upperBound,
								UpperCardinalityTerms.constant(lowerBoundCardinality)))
				))
				.action(
						remove(nodeType, p1)
				)
		));
		// No need to create concretization rules here, because the {@code nodeType} is never partial (it is always a
		// class) and any errors during concretization would be caught by the {@code errorSymbol} error predicate.
	}
}
