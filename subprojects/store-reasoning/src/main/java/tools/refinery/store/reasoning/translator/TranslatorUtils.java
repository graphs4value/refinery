/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.Set;

public class TranslatorUtils {
	private TranslatorUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
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
		if (!mergeAll(refiners, key, value)) {
			return false;
		}
		if (oppositeRefiners.length > 0) {
			var oppositeKey = Tuple.of(key.get(1), key.get(0));
			return mergeAll(oppositeRefiners, oppositeKey, value);
		}
		return true;
	}
}
