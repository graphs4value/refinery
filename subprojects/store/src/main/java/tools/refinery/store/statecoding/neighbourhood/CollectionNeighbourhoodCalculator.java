/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighbourhood;

import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.statecoding.StateCodeCalculator;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple0;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

public class CollectionNeighbourhoodCalculator implements StateCodeCalculator {
		protected final List<Interpretation<?>> nullImpactValues;
		protected final LinkedHashMap<Interpretation<?>, long[]> impactValues;

		public CollectionNeighbourhoodCalculator(List<? extends Interpretation<?>> interpretations) {
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
						impact[i] = random.nextLong();
					}
					impactValues.put(interpretation, impact);
				}
			}
		}

		@Override
		public StateCoderResult calculateCodes() {
			ObjectCodeImpl previous = new ObjectCodeImpl();
			ObjectCodeImpl next = new ObjectCodeImpl();

			int previousSize = 1;
			long lastSum;
			boolean grows;

			do{
				for (var impactValueEntry : this.impactValues.entrySet()) {
					Interpretation<?> interpretation = impactValueEntry.getKey();
					long[] impact = impactValueEntry.getValue();
					var cursor = interpretation.getAll();
					while (cursor.move()) {
						Tuple tuple = cursor.getKey();
						Object value = cursor.getValue();
						long tupleHash = getTupleHash(tuple, value, previous);
						addHash(next, tuple, impact, tupleHash);
					}
				}

				previous = next;
				next = null;
				lastSum = 0;
				MutableLongSet codes = new LongHashSet();
				for (int i = 0; i < previous.getSize(); i++) {
					long objectHash = previous.get(i);
					codes.add(objectHash);

					final long shifted1 = objectHash>>> 32;
					final long shifted2 = objectHash << 32;
					lastSum += shifted1 + shifted2;
				}
				int nextSize = codes.size();
				grows = previousSize < nextSize;
				previousSize = nextSize;

				if(grows) {
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

		protected long getTupleHash(Tuple tuple, Object value, ObjectCodeImpl objectCodeImpl) {
			long result = value.hashCode();
			int arity = tuple.getSize();
			if (arity == 1) {
				result = result * 31 + objectCodeImpl.get(tuple.get(0));
			} else if (arity == 2) {
				result = result * 31 + objectCodeImpl.get(tuple.get(0));
				result = result * 31 + objectCodeImpl.get(tuple.get(1));
				if (tuple.get(0) == tuple.get(1)) {
					result++;
				}
			} else if (arity > 2) {
				for (int i = 0; i < arity; i++) {
					result = result * 31 + objectCodeImpl.get(tuple.get(i));
				}
			}
			return result;
		}

		protected void addHash(ObjectCodeImpl objectCodeImpl, Tuple tuple, long[] impact, long tupleHashCode) {
			if (tuple.getSize() == 1) {
				addHash(objectCodeImpl, tuple.get(0), impact[0], tupleHashCode);
			} else if (tuple.getSize() == 2) {
				addHash(objectCodeImpl, tuple.get(0), impact[0], tupleHashCode);
				addHash(objectCodeImpl, tuple.get(1), impact[1], tupleHashCode);
			} else if (tuple.getSize() > 2) {
				for (int i = 0; i < tuple.getSize(); i++) {
					addHash(objectCodeImpl, tuple.get(i), impact[i], tupleHashCode);
				}
			}
		}

		protected void addHash(ObjectCodeImpl objectCodeImpl, int o, long impact, long tupleHash) {
			objectCodeImpl.set(o, objectCodeImpl.get(o) + tupleHash * impact);
		}
}
