/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.*;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.intinterval.IntBound;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.realinterval.RealBound;
import tools.refinery.logic.term.realinterval.RealInterval;
import tools.refinery.logic.term.realinterval.RealIntervalDomain;
import tools.refinery.logic.term.realinterval.RoundingMode;
import tools.refinery.logic.term.string.StringDomain;
import tools.refinery.logic.term.string.StringValue;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueDomain;
import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.query.resultset.ResultSetListener;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.tuple.Tuple;

import java.math.BigDecimal;

// Z3 assertions create unchecked generic arrays.
// Moreover, we need to convert `FuncDecl<?>` to the specific Z3 sort for assertions.
@SuppressWarnings("unchecked")
class PartialFunctionMonitor<A extends AbstractValue<A, C>, C> implements ResultSetListener<A> {
	private final RuleBasedSolver ruleBasedSolver;
	private final PartialFunction<A, C> partialFunction;
	private final PartialInterpretation<A, C> partialInterpretation;
	private final PartialInterpretationRefiner<A, C> refiner;
	private final MutableObjectIntMap<Tuple> refCounts;

	public PartialFunctionMonitor(RuleBasedSolver ruleBasedSolver, PartialFunction<A, C> partialFunction) {
		this.ruleBasedSolver = ruleBasedSolver;
		this.partialFunction = partialFunction;
		var context = ruleBasedSolver.getContext();
		this.partialInterpretation = context.getPartialInterpretation(ruleBasedSolver.getConcreteness(),
				partialFunction);
		partialInterpretation.addListener(this);
		this.refiner = context.getRefiner(partialFunction);
		this.refCounts = ObjectIntMaps.mutable.empty();
	}

	@Override
	public void put(Tuple key, A fromValue, A toValue) {
		if (refCounts.containsKey(key)) {
			ruleBasedSolver.markChanged();
		}
	}

	public void addRef(Tuple key) {
		int newValue = refCounts.addToValue(key, 1);
		if (newValue == 1) {
			ruleBasedSolver.markChanged();
		}
	}

	public void removeRef(Tuple key) {
		int newValue = refCounts.addToValue(key, -1);
		if (newValue < 0) {
			throw new IllegalStateException("Invalid reference count for variable %s%s"
					.formatted(partialFunction.name(), key));
		}
		if (newValue == 0) {
			refCounts.remove(key);
		}
	}

	public void addAssertions(Solver solver) {
		var context = ruleBasedSolver.getContext();
		var z3Context = context.getZ3Context();
		for (var entry : refCounts.keyValuesView()) {
			var tuple = entry.getOne();
			var variable = context.getVariable(partialFunction, tuple);
			var value = partialInterpretation.get(tuple);
			addAssertion(z3Context, solver, variable, value);
		}
	}

	private void addAssertion(Context context, Solver solver, FuncDecl<?> variable, A value) {
		switch (value) {
			case TruthValue truthValue -> addAssertion(context, solver, (FuncDecl<BoolSort>) variable, truthValue);
			case IntInterval intValue -> addAssertion(context, solver, (FuncDecl<IntSort>) variable, intValue);
			case RealInterval realValue -> addAssertion(context, solver, (FuncDecl<RealSort>) variable, realValue);
			case StringValue stringValue -> addAssertion(context, solver, (FuncDecl<CharSort>) variable, stringValue);
			default -> throw new IllegalArgumentException("Unknown value %s for partial function %s".formatted(
					value, partialFunction));
		}
	}

	private void addAssertion(Context context, Solver solver, FuncDecl<BoolSort> variable, TruthValue value) {
		switch (value) {
		case UNKNOWN -> {
			// Nothing to assert.
		}
		case TRUE -> solver.add(context.mkConst(variable));
		case FALSE -> solver.add(context.mkNot(context.mkConst(variable)));
		case ERROR -> solver.add(context.mkFalse());
		}
	}

	private void addAssertion(Context context, Solver solver, FuncDecl<IntSort> variable, IntInterval intValue) {
		if (intValue.isError()) {
			solver.add(context.mkFalse());
			return;
		}
		if (intValue.isConcrete()) {
			var concreteValue = intValue.getConcrete();
			if (concreteValue == null) {
				throw new IllegalStateException("Concrete value cannot be null for concrete int interval");
			}
			solver.add(context.mkEq(context.mkConst(variable), context.mkInt(concreteValue.toString(10))));
			return;
		}
		if (intValue.lowerBound() instanceof IntBound.Finite(var lowerBound)) {
			solver.add(context.mkGe(context.mkConst(variable), context.mkInt(lowerBound.toString(10))));
		}
		if (intValue.upperBound() instanceof IntBound.Finite(var upperBound)) {
			solver.add(context.mkLe(context.mkConst(variable), context.mkInt(upperBound.toString(10))));
		}
	}

	private void addAssertion(Context context, Solver solver, FuncDecl<RealSort> variable, RealInterval realValue) {
		if (realValue.isError()) {
			solver.add(context.mkFalse());
			return;
		}
		if (realValue.isConcrete()) {
			var concreteValue = realValue.getConcrete();
			if (concreteValue == null) {
				throw new IllegalStateException("Concrete value cannot be null for concrete real interval");
			}
			solver.add(context.mkEq(context.mkConst(variable), context.mkReal(concreteValue.toString())));
			return;
		}
		if (realValue.lowerBound() instanceof RealBound.Finite(var lowerBound)) {
			solver.add(context.mkGe(context.mkConst(variable), context.mkReal(lowerBound.toString())));
		}
		if (realValue.upperBound() instanceof RealBound.Finite(var upperBound)) {
			solver.add(context.mkLe(context.mkConst(variable), context.mkReal(upperBound.toString())));
		}
	}

	private void addAssertion(Context context, Solver solver, FuncDecl<CharSort> variable, StringValue stringValue) {
		switch (stringValue) {
		case StringValue.Unknown _ -> {
			// Nothing to assert.
		}
		case StringValue.Concrete(var concreteValue) -> solver.add(context.mkEq(context.mkConst(variable),
				context.mkString(concreteValue)));
		case StringValue.Error _ -> solver.add(context.mkFalse());
		}
	}

	public boolean isTracking() {
		return refCounts.notEmpty();
	}

	public PropagationResult refineWithModel(Model model, Object reason) {
		boolean changed = false;
		var context = ruleBasedSolver.getContext();
		for (var entry : refCounts.keyValuesView()) {
			var tuple = entry.getOne();
			var variable = context.getVariable(partialFunction, tuple);
			var z3Value = model.getConstInterp(variable);
			if (z3Value == null) {
				// Any value is valid.
				continue;
			}
			var value = getValue(z3Value, tuple);
			var oldValue = partialInterpretation.get(tuple);
			if (!oldValue.equals(value)) {
				changed = true;
				if (!refiner.merge(tuple, value)) {
					return new PropagationRejectedResult(reason, "Failed to merge");
				}
			}
		}
		return changed ? PropagationResult.PROPAGATED : PropagationResult.UNCHANGED;
	}

	private @NotNull A getValue(Expr<?> z3Value, Tuple tuple) {
		var abstractDomain = partialFunction.abstractDomain();
		A value = null;
		if (TruthValueDomain.INSTANCE.equals(abstractDomain)) {
			value = (A) getTruthValue((Expr<BoolSort>) z3Value);
		} else if (IntIntervalDomain.INSTANCE.equals(abstractDomain)) {
			value = (A) getIntValue((Expr<IntSort>) z3Value);
		} else if (RealIntervalDomain.INSTANCE.equals(abstractDomain)) {
			value = (A) getRealValue((Expr<RealSort>) z3Value);
		} else if (StringDomain.INSTANCE.equals(abstractDomain)) {
			value = (A) getStringValue((Expr<CharSort>) z3Value);
		}
		if (value == null) {
			throw new IllegalStateException("Unknown value %s in SMT model for %s%s".formatted(
					z3Value, partialFunction.name(), tuple));
		}
		return value;
	}

	public TruthValue getTruthValue(Expr<BoolSort> value) {
		if (value.isTrue()) {
			return TruthValue.TRUE;
		}
		if (value.isFalse()) {
			return TruthValue.FALSE;
		}
		return TruthValue.UNKNOWN;
	}

	public IntInterval getIntValue(Expr<IntSort> value) {
		if (value instanceof IntNum intNum) {
			var concreteValue = intNum.getBigInteger();
			return IntInterval.of(concreteValue);
		}
		return null;
	}

	public RealInterval getRealValue(Expr<RealSort> value) {
		if (value instanceof RatNum ratNum) {
			var numerator = new BigDecimal(ratNum.getBigIntNumerator());
			var denominator = new BigDecimal(ratNum.getBigIntDenominator());
			var lowerBound = numerator.divide(denominator, RoundingMode.FLOOR.context());
			var upperBound = numerator.divide(denominator, RoundingMode.CEIL.context());
			return RealInterval.of(lowerBound, upperBound);
		}
		if (value instanceof AlgebraicNum algebraicNum) {
			var precision = RoundingMode.PRECISION + 1;
			var lowerBoundRat = algebraicNum.toLower(precision);
			var lowerBound = new BigDecimal(lowerBoundRat.getBigIntNumerator())
					.divide(new BigDecimal(lowerBoundRat.getBigIntDenominator()), RoundingMode.FLOOR.context());
			var upperBoundRat = algebraicNum.toUpper(precision);
			var upperBound = new BigDecimal(upperBoundRat.getBigIntNumerator())
					.divide(new BigDecimal(upperBoundRat.getBigIntDenominator()), RoundingMode.CEIL.context());
			return RealInterval.of(lowerBound, upperBound);
		}
		return null;
	}

	public StringValue getStringValue(Expr<CharSort> value) {
		if (value.isString()) {
			return StringValue.of(value.getString());
		}
		return null;
	}
}
