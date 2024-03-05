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

public final class BuiltinTermInterpreter extends AbstractTermInterpreter {
	public static final DataExprType BOOLEAN_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "boolean");
	public static final DataExprType INT_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "int");
	public static final DataExprType REAL_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "real");
	public static final DataExprType STRING_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "string");
	public static final AggregatorName MIN_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "min");
	public static final AggregatorName MAX_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "max");
	public static final AggregatorName SUM_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "sum");

	public BuiltinTermInterpreter() {
		addNegation(BOOLEAN_TYPE);
		addBinaryOperator(BinaryOp.AND, BOOLEAN_TYPE);
		addBinaryOperator(BinaryOp.OR, BOOLEAN_TYPE);
		addBinaryOperator(BinaryOp.XOR, BOOLEAN_TYPE);

		addUnaryOperator(UnaryOp.PLUS, INT_TYPE);
		addUnaryOperator(UnaryOp.MINUS, INT_TYPE);
		addComparison(INT_TYPE);
		addRange(INT_TYPE);
		addBinaryOperator(BinaryOp.ADD, INT_TYPE);
		addBinaryOperator(BinaryOp.SUB, INT_TYPE);
		addBinaryOperator(BinaryOp.MUL, INT_TYPE);
		addAggregator(MIN_AGGREGATOR, INT_TYPE);
		addAggregator(MAX_AGGREGATOR, INT_TYPE);
		addAggregator(SUM_AGGREGATOR, INT_TYPE);

		addUnaryOperator(UnaryOp.PLUS, REAL_TYPE);
		addUnaryOperator(UnaryOp.MINUS, REAL_TYPE);
		addCast(INT_TYPE, REAL_TYPE);
		addComparison(REAL_TYPE);
		addRange(REAL_TYPE);
		addBinaryOperator(BinaryOp.ADD, REAL_TYPE);
		addBinaryOperator(BinaryOp.SUB, REAL_TYPE);
		addBinaryOperator(BinaryOp.MUL, REAL_TYPE);
		addBinaryOperator(BinaryOp.DIV, REAL_TYPE);
		addBinaryOperator(BinaryOp.POW, REAL_TYPE);
		addAggregator(MIN_AGGREGATOR, REAL_TYPE);
		addAggregator(MAX_AGGREGATOR, REAL_TYPE);
		addAggregator(SUM_AGGREGATOR, REAL_TYPE);
	}
}
