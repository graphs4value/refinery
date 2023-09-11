/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighbourhood;

import org.eclipse.collections.api.factory.primitive.LongIntMaps;
import org.eclipse.collections.api.map.primitive.LongIntMap;
import org.eclipse.collections.api.map.primitive.MutableLongIntMap;
import org.eclipse.collections.api.set.primitive.IntSet;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.AnyInterpretation;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.statecoding.StateCodeCalculator;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class LazyNeighbourhoodCalculator extends AbstractNeighbourhoodCalculator implements StateCodeCalculator {
	public LazyNeighbourhoodCalculator(Model model, List<? extends AnyInterpretation> interpretations,
									   IntSet individuals) {
		super(model, interpretations, individuals);
	}

	public StateCoderResult calculateCodes() {
		ObjectCodeImpl previousObjectCode = new ObjectCodeImpl();
		MutableLongIntMap prevHash2Amount = LongIntMaps.mutable.empty();

		long lastSum;
		// All hash code is 0, except to the individuals.
		int lastSize = 1;
		boolean first = true;

		boolean grows;
		int rounds = 0;
		do {
			final ObjectCodeImpl nextObjectCode;
			if (first) {
				nextObjectCode = new ObjectCodeImpl();
				initializeWithIndividuals(nextObjectCode);
			} else {
				nextObjectCode = new ObjectCodeImpl(previousObjectCode);
			}
			constructNextObjectCodes(previousObjectCode, nextObjectCode, prevHash2Amount);

			MutableLongIntMap nextHash2Amount = LongIntMaps.mutable.empty();
			lastSum = calculateLastSum(previousObjectCode, nextObjectCode, prevHash2Amount, nextHash2Amount);

			int nextSize = nextHash2Amount.size();
			grows = nextSize > lastSize;
			lastSize = nextSize;
			first = false;

			previousObjectCode = nextObjectCode;
			prevHash2Amount = nextHash2Amount;
		} while (grows && rounds++ < 4/*&& lastSize < previousObjectCode.getSize()*/);

		long result = calculateModelCode(lastSum);

		return new StateCoderResult((int) result, previousObjectCode);
	}

	private long calculateLastSum(ObjectCodeImpl previous, ObjectCodeImpl next, LongIntMap hash2Amount,
								  MutableLongIntMap nextHash2Amount) {
		long lastSum = 0;
		for (int i = 0; i < next.getSize(); i++) {
			final long hash;
			if (isUnique(hash2Amount, previous, i)) {
				hash = previous.get(i);
				next.set(i, hash);
			} else {
				hash = next.get(i);
			}

			final int amount = nextHash2Amount.get(hash);
			nextHash2Amount.put(hash, amount + 1);

			final long shifted1 = hash >>> 8;
			final long shifted2 = hash << 8;
			final long shifted3 = hash >> 2;
			lastSum += shifted1 * shifted3 + shifted2;
		}
		return lastSum;
	}

	private void constructNextObjectCodes(ObjectCodeImpl previous, ObjectCodeImpl next, LongIntMap hash2Amount) {
		for (var impactValueEntry : this.impactValues.entrySet()) {
			Interpretation<?> interpretation = (Interpretation<?>) impactValueEntry.getKey();
			var cursor = interpretation.getAll();
			int arity = interpretation.getSymbol().arity();
			long[] impactValue = impactValueEntry.getValue();

			if (arity == 1) {
				while (cursor.move()) {
					lazyImpactCalculation1(hash2Amount, previous, next, impactValue, cursor);
				}
			} else if (arity == 2) {
				while (cursor.move()) {
					lazyImpactCalculation2(hash2Amount, previous, next, impactValue, cursor);
				}
			} else {
				while (cursor.move()) {
					lazyImpactCalculationN(hash2Amount, previous, next, impactValue, cursor);
				}
			}
		}
	}

	private boolean isUnique(LongIntMap hash2Amount, ObjectCodeImpl objectCodeImpl, int object) {
		final long hash = objectCodeImpl.get(object);
		if (hash == 0) {
			return false;
		}
		final int amount = hash2Amount.get(hash);
		return amount == 1;
	}

	private void lazyImpactCalculation1(LongIntMap hash2Amount, ObjectCodeImpl previous, ObjectCodeImpl next,
										long[] impactValues, Cursor<Tuple, ?> cursor) {

		Tuple tuple = cursor.getKey();
		int o = tuple.get(0);

		if (isUnique(hash2Amount, previous, o)) {
			next.ensureSize(o);
		} else {
			Object value = cursor.getValue();
			long tupleHash = getTupleHash1(tuple, value, previous);

			addHash(next, o, impactValues[0], tupleHash);
		}
	}

	private void lazyImpactCalculation2(LongIntMap hash2Amount, ObjectCodeImpl previous, ObjectCodeImpl next,
										long[] impactValues, Cursor<Tuple, ?> cursor) {
		final Tuple tuple = cursor.getKey();
		final int o1 = tuple.get(0);
		final int o2 = tuple.get(1);

		final boolean u1 = isUnique(hash2Amount, previous, o1);
		final boolean u2 = isUnique(hash2Amount, previous, o2);

		if (u1 && u2) {
			next.ensureSize(o1);
			next.ensureSize(o2);
		} else {
			Object value = cursor.getValue();
			long tupleHash = getTupleHash2(tuple, value, previous);

			if (!u1) {
				addHash(next, o1, impactValues[0], tupleHash);
				next.ensureSize(o2);
			}
			if (!u2) {
				next.ensureSize(o1);
				addHash(next, o2, impactValues[1], tupleHash);
			}
		}
	}

	private void lazyImpactCalculationN(LongIntMap hash2Amount, ObjectCodeImpl previous, ObjectCodeImpl next,
										long[] impactValues, Cursor<Tuple, ?> cursor) {
		final Tuple tuple = cursor.getKey();

		final boolean[] uniques = new boolean[tuple.getSize()];
		boolean allUnique = true;
		for (int i = 0; i < tuple.getSize(); i++) {
			final boolean isUnique = isUnique(hash2Amount, previous, tuple.get(i));
			uniques[i] = isUnique;
			allUnique &= isUnique;
		}

		if (allUnique) {
			for (int i = 0; i < tuple.getSize(); i++) {
				next.ensureSize(tuple.get(i));
			}
		} else {
			Object value = cursor.getValue();
			long tupleHash = getTupleHashN(tuple, value, previous);

			for (int i = 0; i < tuple.getSize(); i++) {
				int o = tuple.get(i);
				if (!uniques[i]) {
					addHash(next, o, impactValues[i], tupleHash);
				} else {
					next.ensureSize(o);
				}
			}
		}
	}

}
