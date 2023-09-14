/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.stateequivalence;

import org.eclipse.collections.api.factory.primitive.IntIntMaps;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import tools.refinery.store.model.AnyInterpretation;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.statecoding.Morphism;
import tools.refinery.store.statecoding.ObjectCode;
import tools.refinery.store.statecoding.StateEquivalenceChecker;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StateEquivalenceCheckerImpl implements StateEquivalenceChecker {
	public static final int LIMIT = 1000;

	@Override
	public EquivalenceResult constructMorphism(IntSet individuals,
											   List<? extends AnyInterpretation> interpretations1,
											   ObjectCode code1,
											   List<? extends AnyInterpretation> interpretations2,
											   ObjectCode code2) {
		MutableIntIntMap object2PermutationGroup = IntIntMaps.mutable.empty();
		List<List<IntIntMap>> permutationsGroups = new ArrayList<>();

		final EquivalenceResult permutations = constructPermutationNavigation(individuals,
				indexByHash(code1, individuals), indexByHash(code2, individuals),
				object2PermutationGroup, permutationsGroups);

		if (permutations == EquivalenceResult.DIFFERENT) {
			return EquivalenceResult.DIFFERENT;
		}

		boolean hasNext;
		PermutationMorphism morphism = new PermutationMorphism(object2PermutationGroup, permutationsGroups);
		int tried = 0;
		do {
			if (testMorphism(interpretations1, interpretations2, morphism)) {
				return permutations;
			}

			if (tried >= LIMIT) {
				return EquivalenceResult.UNKNOWN;
			}

			hasNext = morphism.next();
			tried++;
		} while (hasNext);

		if (permutations == EquivalenceResult.UNKNOWN) {
			return EquivalenceResult.UNKNOWN;
		} else {
			return EquivalenceResult.DIFFERENT;
		}
	}

	private MutableLongObjectMap<MutableIntSet> indexByHash(ObjectCode code, IntSet individuals) {
		MutableLongObjectMap<MutableIntSet> result = LongObjectMaps.mutable.empty();
		for (int o = 0; o < code.getSize(); o++) {
			if (!individuals.contains(o)) {
				long hash = code.get(o);
				if (hash != 0) {
					var equivalenceClass = result.get(hash);
					if (equivalenceClass == null) {
						equivalenceClass = IntSets.mutable.empty();
						result.put(hash, equivalenceClass);
					}
					equivalenceClass.add(o);
				}
			}
		}
		return result;
	}

	private EquivalenceResult constructPermutationNavigation(
			IntSet individuals, MutableLongObjectMap<MutableIntSet> map1, MutableLongObjectMap<MutableIntSet> map2,
			MutableIntIntMap object2OptionIndex, List<List<IntIntMap>> listOfOptions) {
		if (map1.size() != map2.size()) {
			return EquivalenceResult.DIFFERENT;
		}

		var iterator = map1.keySet().longIterator();

		boolean allComplete = true;

		while (iterator.hasNext()) {
			long hash = iterator.next();
			var set1 = map1.get(hash);
			var set2 = map2.get(hash);
			if (set2 == null) {
				return EquivalenceResult.DIFFERENT;
			}

			var pairing = NodePairing.constructNodePairing(set1, set2);
			if (pairing == null) {
				return EquivalenceResult.DIFFERENT;
			}

			allComplete &= pairing.isComplete();

			final int optionIndex = listOfOptions.size();
			set1.forEach(key -> object2OptionIndex.put(key, optionIndex));
			listOfOptions.add(pairing.permutations());
		}

		individuals.forEach(o -> listOfOptions.add(o, List.of(IntIntMaps.immutable.of(o, o))));

		if (allComplete) {
			return EquivalenceResult.ISOMORPHIC;
		} else {
			return EquivalenceResult.UNKNOWN;
		}
	}

	private boolean testMorphism(List<? extends AnyInterpretation> s, List<? extends AnyInterpretation> t,
								 Morphism m) {
		for (int interpretationIndex = 0; interpretationIndex < s.size(); interpretationIndex++) {
			var sI = s.get(interpretationIndex);
			var tI = t.get(interpretationIndex);

			var cursor = ((Interpretation<?>) sI).getAll();
			while (cursor.move()) {
				final Tuple sTuple = cursor.getKey();
				final Object sValue = cursor.getValue();

				final Tuple tTuple = apply(sTuple, m);
				final Object tValue = ((Interpretation<?>) tI).get(tTuple);

				if (!Objects.equals(sValue, tValue)) {
					return false;
				}
			}
		}
		return true;
	}

	private Tuple apply(Tuple t, Morphism m) {
		final int arity = t.getSize();
		if (arity == 0) {
			return Tuple.of();
		} else if (arity == 1) {
			return Tuple.of(m.get(t.get(0)));
		} else if (arity == 2) {
			return Tuple.of(m.get(t.get(0)), m.get(t.get(1)));
		} else {
			int[] newTupleIndices = new int[arity];
			for (int i = 0; i < arity; i++) {
				newTupleIndices[i] = m.get(t.get(i));
			}
			return Tuple.of(newTupleIndices);
		}
	}
}
