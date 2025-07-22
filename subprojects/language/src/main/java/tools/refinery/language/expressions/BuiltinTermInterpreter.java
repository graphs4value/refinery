/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.expressions;

import tools.refinery.language.model.problem.*;
import tools.refinery.logic.term.intinterval.Bound;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.intinterval.IntIntervalTerms;
import tools.refinery.logic.term.string.StringDomain;
import tools.refinery.logic.term.string.StringValue;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueDomain;

import static tools.refinery.language.expressions.BuiltInTerms.*;

public final class BuiltinTermInterpreter extends AbstractTermInterpreter {
	public BuiltinTermInterpreter() {
		addDomain(BOOLEAN_TYPE, TruthValueDomain.INSTANCE, BuiltinTermInterpreter::createLogicConstant);

		addDomain(INT_TYPE, IntIntervalDomain.INSTANCE, (value) -> {
			// We can't express improper intervals where one end is the opposite infinity.
			// This also covers the {@code error} value of +inf..-inf.
			if (Bound.Infinite.POSITIVE_INFINITY.equals(value.lowerBound()) ||
					Bound.Infinite.NEGATIVE_INFINITY.equals(value.upperBound())) {
				return createLogicConstant(TruthValue.ERROR);
			}
			if (IntInterval.UNKNOWN.equals(value)) {
				return createLogicConstant(TruthValue.UNKNOWN);
			}
			if (value.lowerBound() instanceof Bound.Finite finiteLowerBound &&
					finiteLowerBound.equals(value.upperBound())) {
				return createIntConstant(finiteLowerBound.value());
			}
			var range = ProblemFactory.eINSTANCE.createRangeExpr();
			range.setLeft(boundToConstant(value.lowerBound()));
			range.setRight(boundToConstant(value.upperBound()));
			return range;
		});
		addAggregator(SUM_AGGREGATOR, INT_TYPE, INT_TYPE, IntIntervalTerms.INT_SUM);
		addAggregator(MIN_AGGREGATOR, INT_TYPE, INT_TYPE, IntIntervalTerms.INT_MIN);
		addAggregator(MAX_AGGREGATOR, INT_TYPE, INT_TYPE, IntIntervalTerms.INT_MAX);

		addDomain(STRING_TYPE, StringDomain.INSTANCE, (value) -> switch (value) {
			case StringValue.Unknown ignored -> createLogicConstant(TruthValue.UNKNOWN);
			case StringValue.Error ignored -> createLogicConstant(TruthValue.ERROR);
			case StringValue.Concrete concreteValue -> createStringConstant(concreteValue.value());
		});
	}

	public static LogicConstant createLogicConstant(TruthValue value) {
		var constant = ProblemFactory.eINSTANCE.createLogicConstant();
		constant.setLogicValue(switch (value) {
			case TRUE -> LogicValue.TRUE;
			case FALSE -> LogicValue.FALSE;
			case UNKNOWN -> LogicValue.UNKNOWN;
			case ERROR -> LogicValue.ERROR;
		});
		return constant;
	}

	public static IntConstant createIntConstant(int value) {
		var constant = ProblemFactory.eINSTANCE.createIntConstant();
		constant.setIntValue(value);
		return constant;
	}

	private static Expr boundToConstant(Bound bound) {
		return switch (bound) {
			case Bound.Infinite ignoredInfiniteBound -> ProblemFactory.eINSTANCE.createInfiniteConstant();
			case Bound.Finite finiteBound -> createIntConstant(finiteBound.value());
		};
	}

	public static StringConstant createStringConstant(String value) {
		var constant = ProblemFactory.eINSTANCE.createStringConstant();
		constant.setStringValue(value);
		return constant;
	}
}
