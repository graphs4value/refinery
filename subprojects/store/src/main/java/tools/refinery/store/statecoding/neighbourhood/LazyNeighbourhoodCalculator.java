/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighbourhood;

import org.eclipse.collections.api.map.primitive.LongIntMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.statecoding.StateCodeCalculator;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple0;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

public class LazyNeighbourhoodCalculator implements StateCodeCalculator {
	protected final List<Interpretation<?>> nullImpactValues;
	protected final LinkedHashMap<Interpretation<?>, long[]> impactValues;

	public LazyNeighbourhoodCalculator(List<? extends Interpretation<?>> interpretations) {
		this.nullImpactValues = new ArrayList<>();
		this.impactValues = new LinkedHashMap<>();
		Random random = new Random(1);

		for (Interpretation<?> interpretation : interpretations) {
			int arity = interpretation.getSymbol().arity();
			if (arity == 0) {
				nullImpactValues.add(interpretation);
			} else {
				long[] impact = new long[arity];
				for (int i = 0; i < arity; i++) {
					impact[i] = random.nextInt();
				}
				impactValues.put(interpretation, impact);
			}
		}
	}

	public StateCoderResult calculateCodes() {
		ObjectCodeImpl previous = new ObjectCodeImpl();
		ObjectCodeImpl next = new ObjectCodeImpl();
		LongIntMap hash2Amount = new LongIntHashMap();

		long lastSum;
		int lastSize = 1;
		boolean grows;

		do {
			constructNextObjectCodes(previous, next, hash2Amount);

			LongIntHashMap nextHash2Amount = new LongIntHashMap();
			lastSum = calculateLastSum(previous, next, hash2Amount, nextHash2Amount);

			previous = next;
			next = null;

			int nextSize = nextHash2Amount.size();
			grows = nextSize > lastSize;
			lastSize = nextSize;

			if (grows) {
				next = new ObjectCodeImpl(previous);
			}
		} while (grows);

		long result = 1;
		for (var nullImpactValue : nullImpactValues) {
			result = result * 31 + nullImpactValue.get(Tuple0.INSTANCE).hashCode();
		}
		result += lastSum;

		return new StateCoderResult((int) result, previous);
	}

	private long calculateLastSum(ObjectCodeImpl previous, ObjectCodeImpl next, LongIntMap hash2Amount,
								  LongIntHashMap nextHash2Amount) {
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
			lastSum += shifted1*shifted3 + shifted2;
		}
		return lastSum;
	}

	private void constructNextObjectCodes(ObjectCodeImpl previous, ObjectCodeImpl next, LongIntMap hash2Amount) {
		for (var impactValueEntry : this.impactValues.entrySet()) {
			Interpretation<?> interpretation = impactValueEntry.getKey();
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
		if(hash == 0) {
			return false;
		}
		final int amount = hash2Amount.get(hash);
		return amount == 1;
	}

	private void lazyImpactCalculation1(LongIntMap hash2Amount, ObjectCodeImpl previous, ObjectCodeImpl next, long[] impactValues, Cursor<Tuple, ?> cursor) {

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

	private void lazyImpactCalculation2(LongIntMap hash2Amount, ObjectCodeImpl previous, ObjectCodeImpl next, long[] impactValues, Cursor<Tuple, ?> cursor) {
		Tuple tuple = cursor.getKey();
		int o1 = tuple.get(0);
		int o2 = tuple.get(1);

		boolean u1 = isUnique(hash2Amount, previous, o1);
		boolean u2 = isUnique(hash2Amount, previous, o2);

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

	private void lazyImpactCalculationN(LongIntMap hash2Amount, ObjectCodeImpl previous, ObjectCodeImpl next, long[] impactValues, Cursor<Tuple, ?> cursor) {
		Tuple tuple = cursor.getKey();

		boolean[] uniques = new boolean[tuple.getSize()];
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

	private long getTupleHash1(Tuple tuple, Object value, ObjectCodeImpl objectCodeImpl) {
		long result = value.hashCode();
		result = result * 31 + objectCodeImpl.get(tuple.get(0));
		return result;
	}

	private long getTupleHash2(Tuple tuple, Object value, ObjectCodeImpl objectCodeImpl) {
		long result = value.hashCode();
		result = result * 31 + objectCodeImpl.get(tuple.get(0));
		result = result * 31 + objectCodeImpl.get(tuple.get(1));
		if (tuple.get(0) == tuple.get(1)) {
			result*=31;
		}
		return result;
	}

	private long getTupleHashN(Tuple tuple, Object value, ObjectCodeImpl objectCodeImpl) {
		long result = value.hashCode();
		for (int i = 0; i < tuple.getSize(); i++) {
			result = result * 31 + objectCodeImpl.get(tuple.get(i));
		}
		return result;
	}

	protected void addHash(ObjectCodeImpl objectCodeImpl, int o, long impact, long tupleHash) {
		long x = tupleHash * impact;
		objectCodeImpl.set(o, objectCodeImpl.get(o) + x);
	}
}
