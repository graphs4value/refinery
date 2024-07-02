/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.QueryBuilder;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.uppercardinality.FiniteUpperCardinality;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;
import tools.refinery.logic.term.uppercardinality.UpperCardinalityTerms;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.reasoning.literal.CountCandidateLowerBoundLiteral;
import tools.refinery.store.reasoning.literal.CountLowerBoundLiteral;
import tools.refinery.store.reasoning.literal.CountUpperBoundLiteral;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;

import java.util.ArrayList;
import java.util.List;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.int_.IntTerms.constant;
import static tools.refinery.logic.term.int_.IntTerms.less;
import static tools.refinery.store.reasoning.ReasoningAdapter.EQUALS_SYMBOL;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;

public class CrossReferenceUtils {
	private CrossReferenceUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static RelationalQuery createMayHelper(PartialRelation linkType, PartialRelation type,
												  Multiplicity multiplicity, boolean inverse) {
		var preparedBuilder = prepareBuilder(linkType, inverse);
		var literals = new ArrayList<Literal>();
		literals.add(may(type.call(preparedBuilder.variable())));
		if (multiplicity.multiplicity().upperBound() instanceof FiniteUpperCardinality(var finiteUpperBound)) {
			var existingLinks = Variable.of("existingLinks", Integer.class);
			literals.add(new CountLowerBoundLiteral(existingLinks, linkType, preparedBuilder.arguments()));
			literals.add(check(less(existingLinks, constant(finiteUpperBound))));
		}
		return preparedBuilder.builder().clause(literals).build();
	}

	public static RelationalQuery createCandidateMayHelper(PartialRelation linkType, PartialRelation type,
														   Multiplicity multiplicity, boolean inverse) {
		var preparedBuilder = prepareBuilder(linkType, inverse);
		var literals = new ArrayList<Literal>();
		literals.add(candidateMay(type.call(preparedBuilder.variable())));
		if (multiplicity.multiplicity().upperBound() instanceof FiniteUpperCardinality(var finiteUpperBound)) {
			var existingLinks = Variable.of("existingLinks", Integer.class);
			literals.add(new CountCandidateLowerBoundLiteral(existingLinks, linkType, preparedBuilder.arguments()));
			literals.add(check(less(existingLinks, constant(finiteUpperBound))));
		}
		return preparedBuilder.builder().clause(literals).build();
	}

	private record PreparedBuilder(QueryBuilder builder, NodeVariable variable, List<Variable> arguments) {
	}

	private static PreparedBuilder prepareBuilder(PartialRelation linkType, boolean inverse) {
		String name;
		NodeVariable variable;
		List<Variable> arguments;
		if (inverse) {
			name = "Target";
			variable = Variable.of("target");
			arguments = List.of(Variable.of("source"), variable);
		} else {
			name = "Source";
			variable = Variable.of("source");
			arguments = List.of(variable, Variable.of("target"));
		}
		var builder = Query.builder(linkType.name() + "#mayNew" + name);
		builder.parameter(variable);
		return new PreparedBuilder(builder, variable, arguments);
	}

	public static void configureSourceLowerBound(PartialRelation linkType, PartialRelation sourceType,
												 int lowerBound, PropagationBuilder propagationBuilder) {
		var name = linkType.name();
		var lowerBoundCardinality = UpperCardinalities.atMost(lowerBound);
		propagationBuilder.rule(Rule.of(name + "#lowerSource", (builder, p1, p2) -> builder
				.clause(UpperCardinality.class, upperBound -> List.of(
						must(sourceType.call(p1)),
						new CountUpperBoundLiteral(upperBound, linkType, List.of(p1, Variable.of())),
						check(UpperCardinalityTerms.lessEq(upperBound,
								UpperCardinalityTerms.constant(lowerBoundCardinality))),
						may(linkType.call(p1, p2)),
						not(must(linkType.call(p1, p2)))
				))
				.action(
						add(linkType, p1, p2)
				)
		));
		propagationBuilder.rule(Rule.of(name + "#missingTarget", (builder, p1) -> builder
				.clause(UpperCardinality.class, upperBound -> List.of(
						may(sourceType.call(p1)),
						// Violation of monotonicity: stop the propagation of inconsistencies, since the
						// {@code invalidMultiplicity} pattern will already mark the model as invalid.
						not(must(sourceType.call(p1))),
						new CountUpperBoundLiteral(upperBound, linkType, List.of(p1, Variable.of())),
						check(UpperCardinalityTerms.less(upperBound,
								UpperCardinalityTerms.constant(lowerBoundCardinality)))
				))
				.action(
						remove(sourceType, p1)
				)
		));
	}

	public static void configureTargetLowerBound(PartialRelation linkType, PartialRelation targetType,
												 int lowerBound, PropagationBuilder propagationBuilder) {
		var name = linkType.name();
		var lowerBoundCardinality = UpperCardinalities.atMost(lowerBound);
		propagationBuilder.rule(Rule.of(name + "#lowerTarget", (builder, p1, p2) -> builder
				.clause(UpperCardinality.class, upperBound -> List.of(
						must(targetType.call(p2)),
						new CountUpperBoundLiteral(upperBound, linkType, List.of(Variable.of(), p2)),
						check(UpperCardinalityTerms.lessEq(upperBound,
								UpperCardinalityTerms.constant(lowerBoundCardinality))),
						may(linkType.call(p1, p2)),
						not(must(linkType.call(p1, p2)))
				))
				.action(
						add(linkType, p1, p2)
				)
		));
		propagationBuilder.rule(Rule.of(name + "#missingSource", (builder, p1) -> builder
				.clause(UpperCardinality.class, upperBound -> List.of(
						may(targetType.call(p1)),
						// Violation of monotonicity: stop the propagation of inconsistencies, since the
						// {@code invalidMultiplicity} pattern will already mark the model as invalid.
						not(must(targetType.call(p1))),
						new CountUpperBoundLiteral(upperBound, linkType, List.of(Variable.of(), p1)),
						check(UpperCardinalityTerms.less(upperBound,
								UpperCardinalityTerms.constant(lowerBoundCardinality)))
				))
				.action(
						remove(targetType, p1)
				)
		));
	}
}
