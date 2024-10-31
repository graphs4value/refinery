/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.QueryBuilder;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.uppercardinality.FiniteUpperCardinality;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.CountCandidateLowerBoundLiteral;
import tools.refinery.store.reasoning.literal.CountLowerBoundLiteral;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.term.int_.IntTerms.constant;
import static tools.refinery.logic.term.int_.IntTerms.less;
import static tools.refinery.store.reasoning.literal.PartialLiterals.candidateMay;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;

class CrossReferenceUtils {
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

	public static Dnf createSupersetHelper(PartialRelation linkType, Set<PartialRelation> supersets) {
		return createSupersetHelper(linkType, supersets, Set.of());
	}

	public static Dnf createSupersetHelper(PartialRelation linkType, Set<PartialRelation> supersets,
										   Set<PartialRelation> oppositeSupersets) {
		int supersetCount = supersets.size();
		int oppositeSupersetCount = oppositeSupersets.size();
		int literalCount = supersetCount + oppositeSupersetCount;
		var direction = literalCount >= 1 ? ParameterDirection.OUT : ParameterDirection.IN;
		return Dnf.of(linkType.name() + "#superset", builder -> {
			var p1 = builder.parameter("p1", direction);
			var p2 = builder.parameter("p2", direction);
			var literals = new ArrayList<Literal>(literalCount);
			for (PartialRelation superset : supersets) {
				literals.add(superset.call(p1, p2));
			}
			for (PartialRelation oppositeSuperset : oppositeSupersets) {
				literals.add(oppositeSuperset.call(p2, p1));
			}
			builder.clause(literals);
		});
	}

	public static PartialInterpretationRefiner<TruthValue, Boolean>[] getRefiners(ReasoningAdapter adapter,
																				  Set<PartialRelation> relations) {
		// Creation of array with generic member type.
		@SuppressWarnings("unchecked")
		var refiners = (PartialInterpretationRefiner<TruthValue, Boolean>[])
				new PartialInterpretationRefiner<?, ?>[relations.size()];
		var i = 0;
		for (var relation : relations) {
			refiners[i] = adapter.getRefiner(relation);
			i++;
		}
		return refiners;
	}

	public static <A extends AbstractValue<A, C>, C> boolean mergeAll(
			PartialInterpretationRefiner<A, C>[] refiners, Tuple key, A value) {
		int count = refiners.length;
		// Use classic for loop to avoid allocating an iterator.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < count; i++) {
			var refiner = refiners[i];
			if (!refiner.merge(key, value)) {
				return false;
			}
		}
		return true;
	}

	public static <A extends AbstractValue<A, C>, C> boolean mergeAll(
			PartialInterpretationRefiner<A, C>[] refiners, PartialInterpretationRefiner<A, C>[] oppositeRefiners,
			Tuple key, A value) {
		if (!CrossReferenceUtils.mergeAll(refiners, key, value)) {
			return false;
		}
		if (oppositeRefiners.length > 0) {
			var oppositeKey = Tuple.of(key.get(1), key.get(0));
			return CrossReferenceUtils.mergeAll(oppositeRefiners, oppositeKey, value);
		}
		return true;
	}
}
