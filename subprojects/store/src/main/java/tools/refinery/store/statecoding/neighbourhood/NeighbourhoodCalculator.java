/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighbourhood;

import org.eclipse.collections.api.set.primitive.IntSet;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.statecoding.ObjectCode;
import tools.refinery.store.statecoding.StateCodeCalculator;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple0;

import java.util.List;
import java.util.Objects;

public class NeighbourhoodCalculator extends AbstractNeighbourhoodCalculator implements StateCodeCalculator {
	private ObjectCodeImpl previousObjectCode = new ObjectCodeImpl();
	private ObjectCodeImpl nextObjectCode = new ObjectCodeImpl();

	public NeighbourhoodCalculator(Model model, List<? extends Interpretation<?>> interpretations, IntSet individuals) {
		super(model, interpretations, individuals);
	}

	public StateCoderResult calculateCodes() {
		model.checkCancelled();
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
		} while (rounds <= 7 && rounds <= previousObjectCode.getEffectiveSize());

		long result = calculateLastSum(previousObjectCode);
		return new StateCoderResult((int) result, previousObjectCode);
	}

	private long calculateLastSum(ObjectCode codes) {
		long result = 0;
		for (var nullImpactValue : nullImpactValues) {
			result = result * PRIME + Objects.hashCode(((Interpretation<?>) nullImpactValue).get(Tuple0.INSTANCE));
		}

		for (int i = 0; i < codes.getSize(); i++) {
			final long hash = codes.get(i);
			result += hash*PRIME;
		}

		return result;
	}

	private void constructNextObjectCodes(ObjectCodeImpl previous, ObjectCodeImpl next) {
		for (var impactValueEntry : this.impactValues.entrySet()) {
			model.checkCancelled();
			Interpretation<?> interpretation = (Interpretation<?>) impactValueEntry.getKey();
			var cursor = interpretation.getAll();
			int arity = interpretation.getSymbol().arity();
			long[] impactValue = impactValueEntry.getValue();

			if (arity == 1) {
				while (cursor.move()) {
					impactCalculation1(previous, next, impactValue, cursor);
				}
			} else if (arity == 2) {
				while (cursor.move()) {
					impactCalculation2(previous, next, impactValue, cursor);
				}
			} else {
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
}
