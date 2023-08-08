/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighbourhood;

import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.map.mutable.primitive.IntLongHashMap;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.statecoding.ObjectCode;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple0;

import java.util.*;

public abstract class AbstractNeighbourhoodCalculator {
	protected final List<Interpretation<?>> nullImpactValues;
	protected final LinkedHashMap<Interpretation<?>, long[]> impactValues;
	protected final IntLongHashMap individualHashValues;

	protected static final long PRIME = 31;

	protected AbstractNeighbourhoodCalculator(List<? extends Interpretation<?>> interpretations, IntSet individuals) {
		this.nullImpactValues = new ArrayList<>();
		this.impactValues = new LinkedHashMap<>();
		@SuppressWarnings("squid:S2245")
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

	protected void initializeWithIndividuals(ObjectCodeImpl previous) {
		for (var entry : individualHashValues.keyValuesView()) {
			previous.set(entry.getOne(), entry.getTwo());
		}
	}

	protected long getTupleHash1(Tuple tuple, Object value, ObjectCode objectCodeImpl) {
		long result = Objects.hashCode(value);
		result = result * PRIME + objectCodeImpl.get(tuple.get(0));
		return result;
	}

	protected long getTupleHash2(Tuple tuple, Object value, ObjectCode objectCodeImpl) {
		long result = Objects.hashCode(value);
		result = result * PRIME + objectCodeImpl.get(tuple.get(0));
		result = result * PRIME + objectCodeImpl.get(tuple.get(1));
		if (tuple.get(0) == tuple.get(1)) {
			result += PRIME;
			result *= PRIME;
		}
		return result;
	}

	protected long getTupleHashN(Tuple tuple, Object value, ObjectCode objectCodeImpl) {
		long result = Objects.hashCode(value);
		for (int i = 0; i < tuple.getSize(); i++) {
			result = result * PRIME + objectCodeImpl.get(tuple.get(i));
		}
		return result;
	}

	protected void addHash(ObjectCodeImpl objectCodeImpl, int o, long impact, long tupleHash) {
		long x = tupleHash * impact;
		objectCodeImpl.set(o, objectCodeImpl.get(o) + x);
	}

	protected long calculateModelCode(long lastSum) {
		long result = 0;
		for (var nullImpactValue : nullImpactValues) {
			result = result * PRIME + Objects.hashCode(nullImpactValue.get(Tuple0.INSTANCE));
		}
		result += lastSum;
		return result;
	}
}
