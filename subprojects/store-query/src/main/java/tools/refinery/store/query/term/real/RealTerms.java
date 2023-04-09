/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.real;

import tools.refinery.store.query.term.*;

public final class RealTerms {
	public static final Aggregator<Double, Double> REAL_SUM = RealSumAggregator.INSTANCE;
	public static final Aggregator<Double, Double> REAL_MIN = RealExtremeValueAggregator.MINIMUM;
	public static final Aggregator<Double, Double> REAL_MAX = RealExtremeValueAggregator.MAXIMUM;

	private RealTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static ConstantTerm<Double> constant(double value) {
		return new ConstantTerm<>(Double.class, value);
	}

	public static RealArithmeticUnaryTerm plus(Term<Double> body) {
		return new RealArithmeticUnaryTerm(ArithmeticUnaryOperator.PLUS, body);
	}

	public static RealArithmeticUnaryTerm minus(Term<Double> body) {
		return new RealArithmeticUnaryTerm(ArithmeticUnaryOperator.MINUS, body);
	}

	public static RealArithmeticBinaryTerm add(Term<Double> left, Term<Double> right) {
		return new RealArithmeticBinaryTerm(ArithmeticBinaryOperator.ADD, left, right);
	}

	public static RealArithmeticBinaryTerm sub(Term<Double> left, Term<Double> right) {
		return new RealArithmeticBinaryTerm(ArithmeticBinaryOperator.SUB, left, right);
	}

	public static RealArithmeticBinaryTerm mul(Term<Double> left, Term<Double> right) {
		return new RealArithmeticBinaryTerm(ArithmeticBinaryOperator.MUL, left, right);
	}

	public static RealArithmeticBinaryTerm div(Term<Double> left, Term<Double> right) {
		return new RealArithmeticBinaryTerm(ArithmeticBinaryOperator.DIV, left, right);
	}

	public static RealArithmeticBinaryTerm pow(Term<Double> left, Term<Double> right) {
		return new RealArithmeticBinaryTerm(ArithmeticBinaryOperator.POW, left, right);
	}

	public static RealArithmeticBinaryTerm min(Term<Double> left, Term<Double> right) {
		return new RealArithmeticBinaryTerm(ArithmeticBinaryOperator.MIN, left, right);
	}

	public static RealArithmeticBinaryTerm max(Term<Double> left, Term<Double> right) {
		return new RealArithmeticBinaryTerm(ArithmeticBinaryOperator.MAX, left, right);
	}

	public static RealComparisonTerm eq(Term<Double> left, Term<Double> right) {
		return new RealComparisonTerm(ComparisonOperator.EQ, left, right);
	}

	public static RealComparisonTerm notEq(Term<Double> left, Term<Double> right) {
		return new RealComparisonTerm(ComparisonOperator.NOT_EQ, left, right);
	}

	public static RealComparisonTerm less(Term<Double> left, Term<Double> right) {
		return new RealComparisonTerm(ComparisonOperator.LESS, left, right);
	}

	public static RealComparisonTerm lessEq(Term<Double> left, Term<Double> right) {
		return new RealComparisonTerm(ComparisonOperator.LESS_EQ, left, right);
	}

	public static RealComparisonTerm greater(Term<Double> left, Term<Double> right) {
		return new RealComparisonTerm(ComparisonOperator.GREATER, left, right);
	}

	public static RealComparisonTerm greaterEq(Term<Double> left, Term<Double> right) {
		return new RealComparisonTerm(ComparisonOperator.GREATER_EQ, left, right);
	}

	public static IntToRealTerm asReal(Term<Integer> body) {
		return new IntToRealTerm(body);
	}
}
