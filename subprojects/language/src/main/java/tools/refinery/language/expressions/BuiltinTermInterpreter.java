/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.expressions;

import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.BinaryOp;
import tools.refinery.language.model.problem.UnaryOp;
import tools.refinery.language.typesystem.AggregatorName;
import tools.refinery.language.typesystem.DataExprType;
import tools.refinery.language.utils.BuiltinSymbols;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.intinterval.IntIntervalTerms;
import tools.refinery.logic.term.truthvalue.TruthValueDomain;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;

public final class BuiltinTermInterpreter extends AbstractTermInterpreter {
	public static final DataExprType BOOLEAN_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			BuiltinSymbols.BOOLEAN_NAME);
	public static final DataExprType INT_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			BuiltinSymbols.INT_NAME);
	public static final DataExprType REAL_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			BuiltinSymbols.REAL_NAME);
	public static final DataExprType STRING_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			BuiltinSymbols.STRING_NAME);
	public static final AggregatorName MIN_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "min");
	public static final AggregatorName MAX_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "max");
	public static final AggregatorName SUM_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "sum");

	public BuiltinTermInterpreter() {
		addNegation(BOOLEAN_TYPE, TruthValueTerms::not);
		addBinaryOperator(BinaryOp.AND, BOOLEAN_TYPE);
		addBinaryOperator(BinaryOp.OR, BOOLEAN_TYPE);
		addBinaryOperator(BinaryOp.XOR, BOOLEAN_TYPE);
		addDomain(BOOLEAN_TYPE, TruthValueDomain.INSTANCE);

		addDomain(INT_TYPE, IntIntervalDomain.INSTANCE);
		addUnaryOperator(UnaryOp.PLUS, INT_TYPE);
		addUnaryOperator(UnaryOp.MINUS, INT_TYPE);
		this.<IntInterval>addComparison(INT_TYPE, (op, left, right) -> switch (op) {
			case LESS -> IntIntervalTerms.less(left, right);
			case LESS_EQ -> IntIntervalTerms.lessEq(left, right);
			case GREATER -> IntIntervalTerms.greater(left, right);
			case GREATER_EQ -> IntIntervalTerms.greaterEq(left, right);
			case EQ -> IntIntervalTerms.equals(left, right);
			case IN -> IntIntervalTerms.in(left, right);
			default -> throw new IllegalArgumentException("Unsupported comparison");
		});
		this.<IntInterval>addBinaryOperator(INT_TYPE, (op, left, right) -> switch (op) {
			case ADD -> IntIntervalTerms.add(left, right);
			case SUB -> IntIntervalTerms.sub(left, right);
			case MUL -> IntIntervalTerms.mul(left, right);
			default -> throw new IllegalArgumentException("Unsupported binary operation");
		});
		addRange(INT_TYPE, IntIntervalTerms::intInterval);
		addAggregator(MIN_AGGREGATOR, INT_TYPE);
		addAggregator(MAX_AGGREGATOR, INT_TYPE);
		addAggregator(SUM_AGGREGATOR, INT_TYPE);
	}
}
