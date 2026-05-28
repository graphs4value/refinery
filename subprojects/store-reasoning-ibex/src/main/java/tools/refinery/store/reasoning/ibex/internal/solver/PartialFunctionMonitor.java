/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex.internal.solver;

import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.intinterval.IntBound;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.realinterval.RealBound;
import tools.refinery.logic.term.realinterval.RealInterval;
import tools.refinery.logic.term.realinterval.RealIntervalDomain;
import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.query.resultset.ResultSetListener;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.tuple.Tuple;

import java.math.BigDecimal;
import java.math.RoundingMode;

class PartialFunctionMonitor<A extends AbstractValue<A, C>, C> implements ResultSetListener<A> {
	private static final String REJECTION_EMPTY = "IBEX contracted a domain to empty";

	private final IbexSolver ibexSolver;
	private final PartialFunction<A, C> partialFunction;
	private final PartialInterpretation<A, C> interpretation;
	private final PartialInterpretationRefiner<A, C> refiner;
	private final MutableObjectIntMap<Tuple> refCounts = ObjectIntMaps.mutable.empty();

	PartialFunctionMonitor(IbexSolver ibexSolver, PartialFunction<A, C> partialFunction,
						   ReasoningAdapter reasoningAdapter) {
		this.ibexSolver = ibexSolver;
		this.partialFunction = partialFunction;
		this.interpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, partialFunction);
		this.refiner = reasoningAdapter.getRefiner(partialFunction);
		interpretation.addListener(this);
	}

	void addRef(Tuple key) {
		int newCount = refCounts.addToValue(key, 1);
		if (newCount == 1) {
			ibexSolver.markChanged();
		}
	}

	void removeRef(Tuple key) {
		int newCount = refCounts.addToValue(key, -1);
		if (newCount < 0) {
			throw new IllegalStateException(
					"Negative reference count for %s%s".formatted(partialFunction.name(), key));
		}
		if (newCount == 0) {
			refCounts.remove(key);
		}
	}

	boolean fillDomain(Tuple key, double[] domains, int idx) {
		var value = interpretation.get(key);
		return switch ((Object) value) {
			case IntInterval iv -> {
				if (iv.isError()) yield false;
				domains[2 * idx] = intBoundToDouble(iv.lowerBound(), Double.NEGATIVE_INFINITY);
				domains[2 * idx + 1] = intBoundToDouble(iv.upperBound(), Double.POSITIVE_INFINITY);
				yield true;
			}
			case RealInterval rv -> {
				if (rv.isError()) yield false;
				domains[2 * idx] = realBoundToDouble(rv.lowerBound(), Double.NEGATIVE_INFINITY);
				domains[2 * idx + 1] = realBoundToDouble(rv.upperBound(), Double.POSITIVE_INFINITY);
				yield true;
			}
			default -> throw new IllegalArgumentException(
					"IBEX: unsupported value type " + value.getClass().getName());
		};
	}


	@SuppressWarnings("unchecked")
	PropagationResult applyDomain(Tuple key, double[] domains, int idx, Object reason) {
		var abstractDomain = partialFunction.abstractDomain();
		A newValue;
		if (IntIntervalDomain.INSTANCE.equals(abstractDomain)) {
			newValue = (A) doubleToIntInterval(domains[2 * idx], domains[2 * idx + 1]);
		} else if (RealIntervalDomain.INSTANCE.equals(abstractDomain)) {
			newValue = (A) doubleToRealInterval(domains[2 * idx], domains[2 * idx + 1]);
		} else {
			return PropagationResult.UNCHANGED;
		}

		if (newValue.isError()) {
			return new PropagationRejectedResult(reason, REJECTION_EMPTY);
		}
		var oldValue = interpretation.get(key);
		if (oldValue.equals(newValue)) {
			return PropagationResult.UNCHANGED;
		}
		if (!refiner.merge(key, newValue)) {
			return new PropagationRejectedResult(reason, REJECTION_EMPTY);
		}
		return PropagationResult.PROPAGATED;
	}

	@Override
	public void put(Tuple key, A fromValue, A toValue) {
		if (refCounts.containsKey(key)) {
			ibexSolver.markChanged();
		}
	}

	private static double intBoundToDouble(IntBound bound, double infinityValue) {
		return bound instanceof IntBound.Finite(var v) ? v.doubleValue() : infinityValue;
	}

	private static double realBoundToDouble(RealBound bound, double infinityValue) {
		return bound instanceof RealBound.Finite(var v) ? v.doubleValue() : infinityValue;
	}

	private static IntInterval doubleToIntInterval(double lo, double hi) {
		if (Double.isNaN(lo) || Double.isNaN(hi) || lo > hi) {
			return IntInterval.ERROR;
		}
		IntBound lower = Double.isInfinite(lo)
				? IntBound.Infinite.NEGATIVE_INFINITY
				: new IntBound.Finite(BigDecimal.valueOf(lo).setScale(0, RoundingMode.CEILING).toBigInteger());
		IntBound upper = Double.isInfinite(hi)
				? IntBound.Infinite.POSITIVE_INFINITY
				: new IntBound.Finite(BigDecimal.valueOf(hi).setScale(0, RoundingMode.FLOOR).toBigInteger());
		if (lower instanceof IntBound.Finite(var l) && upper instanceof IntBound.Finite(var u)
				&& l.compareTo(u) > 0) {
			return IntInterval.ERROR;
		}
		return IntInterval.of(lower, upper);
	}

	private static RealInterval doubleToRealInterval(double lo, double hi) {
		if (Double.isNaN(lo) || Double.isNaN(hi) || lo > hi) {
			return RealInterval.ERROR;
		}
		RealBound lower = Double.isInfinite(lo)
				? RealBound.Infinite.NEGATIVE_INFINITY
				: new RealBound.Finite(BigDecimal.valueOf(lo));
		RealBound upper = Double.isInfinite(hi)
				? RealBound.Infinite.POSITIVE_INFINITY
				: new RealBound.Finite(BigDecimal.valueOf(hi));
		return RealInterval.of(lower, upper);
	}
}
