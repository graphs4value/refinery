/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighbourhood;

import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.map.mutable.primitive.IntLongHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.statecoding.ObjectCode;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple0;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

public abstract class AbstractNeighbourhoodCalculator {
	protected final List<Interpretation<?>> nullImpactValues;
	protected final LinkedHashMap<Interpretation<?>, long[]> impactValues;
	protected final IntLongHashMap individualHashValues;

	protected AbstractNeighbourhoodCalculator(List<? extends Interpretation<?>> interpretations, IntSet individuals) {
		this.nullImpactValues = new ArrayList<>();
		this.impactValues = new LinkedHashMap<>();
		Random random = new Random(1);

		individualHashValues = new IntLongHashMap();
		var individualsInOrder = individuals.toSortedList(Integer::compare);
		for(int i = 0; i<individualsInOrder.size(); i++) {
			individualHashValues.put(individualsInOrder.get(i), random.nextLong());
		}

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

	protected void initializeWithIndividuals(ObjectCodeImpl previous, LongIntHashMap hash2Amount) {
		for (var entry : individualHashValues.keyValuesView()) {
			previous.set(entry.getOne(), entry.getTwo());
			hash2Amount.put(entry.getTwo(), 1);
		}
	}

	protected long getTupleHash1(Tuple tuple, Object value, ObjectCode objectCodeImpl) {
		long result = value.hashCode();
		result = result * 31 + objectCodeImpl.get(tuple.get(0));
		return result;
	}

	protected long getTupleHash2(Tuple tuple, Object value, ObjectCode objectCodeImpl) {
		long result = value.hashCode();
		result = result * 31 + objectCodeImpl.get(tuple.get(0));
		result = result * 31 + objectCodeImpl.get(tuple.get(1));
		if (tuple.get(0) == tuple.get(1)) {
			result *= 31;
		}
		return result;
	}

	protected long getTupleHashN(Tuple tuple, Object value, ObjectCode objectCodeImpl) {
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

	protected long calculateModelCode(long lastSum) {
		long result = 1;
		for (var nullImpactValue : nullImpactValues) {
			result = result * 31 + nullImpactValue.get(Tuple0.INSTANCE).hashCode();
		}
		result += lastSum;
		return result;
	}
}
