/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighborhood;

import org.eclipse.collections.api.factory.primitive.IntLongMaps;
import org.eclipse.collections.api.map.primitive.MutableIntLongMap;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Model;
import tools.refinery.store.statecoding.ObjectCode;
import tools.refinery.store.statecoding.StateCodeCalculator;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.tuple.Tuple;

import java.util.*;

public abstract class AbstractNeighborhoodCalculator<T> implements StateCodeCalculator {
	private static final long MURMUR64_MAGIC_VALUE = 0xc6a4a7935bd1e995L;
	private static final int MURMUR64_SHIFT = 47;

	private final Model model;
	private final IndividualsSet individuals;
	private final int depth;
	private List<T> nullImpactValues;
	private LinkedHashMap<T, long[]> impactValues;
	private MutableIntLongMap individualHashValues;
	private ObjectCodeImpl previousObjectCode = new ObjectCodeImpl();
	private ObjectCodeImpl nextObjectCode = new ObjectCodeImpl();

	protected AbstractNeighborhoodCalculator(Model model, IndividualsSet individuals, int depth) {
		this.model = model;
		this.individuals = individuals;
		this.depth = depth;
	}

	protected Model getModel() {
		return model;
	}

	protected abstract List<T> getInterpretations();

	protected abstract int getArity(T interpretation);

	protected abstract Object getNullValue(T interpretation);

	// We need the wildcard here, because we don't know the value type.
	@SuppressWarnings("squid:S1452")
	protected abstract Cursor<Tuple, ?> getCursor(T interpretation);

	@Override
	public StateCoderResult calculateCodes() {
		model.checkCancelled();
		ensureInitialized();
		previousObjectCode.clear();
		nextObjectCode.clear();
		initializeWithIndividuals(previousObjectCode);

		int rounds = 0;
		do {
			model.checkCancelled();
			constructNextObjectCodes(previousObjectCode, nextObjectCode);
			var tempObjectCode = previousObjectCode;
			previousObjectCode = nextObjectCode;
			nextObjectCode = tempObjectCode;
			nextObjectCode.clear();
			rounds++;
		} while (rounds <= depth && rounds <= previousObjectCode.getEffectiveSize());

		long result = calculateLastSum(previousObjectCode);
		return new StateCoderResult((int) result, previousObjectCode);
	}

	private void ensureInitialized() {
		if (impactValues != null) {
			return;
		}

		nullImpactValues = new ArrayList<>();
		impactValues = new LinkedHashMap<>();
		individualHashValues = IntLongMaps.mutable.empty();
		// Random isn't used for cryptographical purposes but just to assign distinguishable identifiers to symbols.
		@SuppressWarnings("squid:S2245")
		Random random = new Random(1);

		individuals.stream().forEachOrdered(o -> individualHashValues.put(o, random.nextLong()));

		for (var interpretation : getInterpretations()) {
			int arity = getArity(interpretation);
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

	private long calculateLastSum(ObjectCode codes) {
		long result = 0;
		for (var nullImpactValue : nullImpactValues) {
			result = murmur64Scramble(Objects.hashCode(getNullValue(nullImpactValue)), result);
		}

		long accum = 0;
		for (int i = 0; i < codes.getSize(); i++) {
			final long hash = codes.get(i);
			accum += hash;
		}
		result = murmur64Scramble(accum, result);

		return murmur64Finish(result);
	}

	private void constructNextObjectCodes(ObjectCodeImpl previous, ObjectCodeImpl next) {
		for (var impactValueEntry : this.impactValues.entrySet()) {
			model.checkCancelled();
			var interpretation = impactValueEntry.getKey();
			var cursor = getCursor(interpretation);
			int arity = getArity(interpretation);
			long[] impactValue = impactValueEntry.getValue();
			impactCalculation(previous, next, impactValue, cursor, arity);
		}
	}

	protected void impactCalculation(ObjectCodeImpl previous, ObjectCodeImpl next, long[] impactValue,
									 Cursor<Tuple, ?> cursor, int arity) {
		switch (arity) {
		case 1 -> {
			while (cursor.move()) {
				impactCalculation1(previous, next, impactValue, cursor);
			}
		}
		case 2 -> {
			while (cursor.move()) {
				impactCalculation2(previous, next, impactValue, cursor);
			}
		}
		default -> {
			while (cursor.move()) {
				impactCalculationN(previous, next, impactValue, cursor);
			}
		}
		}
	}

	private void impactCalculation1(ObjectCodeImpl previous, ObjectCodeImpl next, long[] impactValues,
									Cursor<Tuple, ?> cursor) {

		Tuple tuple = cursor.getKey();
		int o = tuple.get(0);
		Object value = cursor.getValue();
		long tupleHash = getTupleHash1(tuple, value, previous);
		addHash(next, o, impactValues[0], tupleHash);
	}

	private void impactCalculation2(ObjectCodeImpl previous, ObjectCodeImpl next, long[] impactValues,
									Cursor<Tuple, ?> cursor) {
		final Tuple tuple = cursor.getKey();
		final int o1 = tuple.get(0);
		final int o2 = tuple.get(1);

		Object value = cursor.getValue();
		long tupleHash = getTupleHash2(tuple, value, previous);

		addHash(next, o1, impactValues[0], tupleHash);
		addHash(next, o2, impactValues[1], tupleHash);
	}

	private void impactCalculationN(ObjectCodeImpl previous, ObjectCodeImpl next, long[] impactValues,
									Cursor<Tuple, ?> cursor) {
		final Tuple tuple = cursor.getKey();

		Object value = cursor.getValue();
		long tupleHash = getTupleHashN(tuple, value, previous);

		for (int i = 0; i < tuple.getSize(); i++) {
			addHash(next, tuple.get(i), impactValues[i], tupleHash);
		}
	}

	protected void initializeWithIndividuals(ObjectCodeImpl previous) {
		for (var entry : individualHashValues.keyValuesView()) {
			previous.set(entry.getOne(), entry.getTwo());
		}
	}

	protected long getTupleHash1(Tuple tuple, Object value, ObjectCode objectCodeImpl) {
		long result = murmur64Scramble(Objects.hashCode(value), 0);
		result = murmur64Scramble(objectCodeImpl.get(tuple.get(0)), result);
		return murmur64Finish(result);
	}

	protected long getTupleHash2(Tuple tuple, Object value, ObjectCode objectCodeImpl) {
		long result = murmur64Scramble(Objects.hashCode(value), 0);
		result = murmur64Scramble(objectCodeImpl.get(tuple.get(0)), result);
		result = murmur64Scramble(objectCodeImpl.get(tuple.get(1)), result);
		if (tuple.get(0) == tuple.get(1)) {
			result = murmur64Scramble(result, 1);
		}
		return murmur64Finish(result);
	}

	protected long getTupleHashN(Tuple tuple, Object value, ObjectCode objectCodeImpl) {
		long result = murmur64Scramble(Objects.hashCode(value), 0);
		for (int i = 0; i < tuple.getSize(); i++) {
			result = murmur64Scramble(objectCodeImpl.get(tuple.get(i)), result);
		}
		return murmur64Finish(result);
	}

	protected void addHash(ObjectCodeImpl objectCodeImpl, int o, long impact, long tupleHash) {
		long x = tupleHash * impact;
		objectCodeImpl.set(o, objectCodeImpl.get(o) + x);
	}

	private static long murmur64Scramble(long k, long h) {
		k *= MURMUR64_MAGIC_VALUE;
		k ^= k >>> MURMUR64_SHIFT;
		k *= MURMUR64_MAGIC_VALUE;
		h ^= k;
		h *= MURMUR64_MAGIC_VALUE;
		return h;
	}

	private static long murmur64Finish(long h) {
		h ^= h >>> MURMUR64_SHIFT;
		h *= MURMUR64_MAGIC_VALUE;
		h ^= h >>> MURMUR64_SHIFT;
		return h;
	}
}
