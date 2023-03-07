package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.term.*;

public final class IntTerms {
	public static final Aggregator<Integer, Integer> INT_SUM = IntSumAggregator.INSTANCE;
	public static final Aggregator<Integer, Integer> INT_MIN = IntExtremeValueAggregator.MINIMUM;
	public static final Aggregator<Integer, Integer> INT_MAX = IntExtremeValueAggregator.MAXIMUM;

	private IntTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static ConstantTerm<Integer> constant(int value) {
		return new ConstantTerm<>(Integer.class, value);
	}

	public static IntArithmeticUnaryTerm plus(Term<Integer> body) {
		return new IntArithmeticUnaryTerm(ArithmeticUnaryOperator.PLUS, body);
	}

	public static IntArithmeticUnaryTerm minus(Term<Integer> body) {
		return new IntArithmeticUnaryTerm(ArithmeticUnaryOperator.MINUS, body);
	}

	public static IntArithmeticBinaryTerm add(Term<Integer> left, Term<Integer> right) {
		return new IntArithmeticBinaryTerm(ArithmeticBinaryOperator.ADD, left, right);
	}

	public static IntArithmeticBinaryTerm sub(Term<Integer> left, Term<Integer> right) {
		return new IntArithmeticBinaryTerm(ArithmeticBinaryOperator.SUB, left, right);
	}

	public static IntArithmeticBinaryTerm mul(Term<Integer> left, Term<Integer> right) {
		return new IntArithmeticBinaryTerm(ArithmeticBinaryOperator.MUL, left, right);
	}

	public static IntArithmeticBinaryTerm div(Term<Integer> left, Term<Integer> right) {
		return new IntArithmeticBinaryTerm(ArithmeticBinaryOperator.DIV, left, right);
	}

	public static IntArithmeticBinaryTerm pow(Term<Integer> left, Term<Integer> right) {
		return new IntArithmeticBinaryTerm(ArithmeticBinaryOperator.POW, left, right);
	}

	public static IntArithmeticBinaryTerm min(Term<Integer> left, Term<Integer> right) {
		return new IntArithmeticBinaryTerm(ArithmeticBinaryOperator.MIN, left, right);
	}

	public static IntArithmeticBinaryTerm max(Term<Integer> left, Term<Integer> right) {
		return new IntArithmeticBinaryTerm(ArithmeticBinaryOperator.MAX, left, right);
	}

	public static IntComparisonTerm eq(Term<Integer> left, Term<Integer> right) {
		return new IntComparisonTerm(ComparisonOperator.EQ, left, right);
	}

	public static IntComparisonTerm notEq(Term<Integer> left, Term<Integer> right) {
		return new IntComparisonTerm(ComparisonOperator.NOT_EQ, left, right);
	}

	public static IntComparisonTerm less(Term<Integer> left, Term<Integer> right) {
		return new IntComparisonTerm(ComparisonOperator.LESS, left, right);
	}

	public static IntComparisonTerm lessEq(Term<Integer> left, Term<Integer> right) {
		return new IntComparisonTerm(ComparisonOperator.LESS_EQ, left, right);
	}

	public static IntComparisonTerm greater(Term<Integer> left, Term<Integer> right) {
		return new IntComparisonTerm(ComparisonOperator.GREATER, left, right);
	}

	public static IntComparisonTerm greaterEq(Term<Integer> left, Term<Integer> right) {
		return new IntComparisonTerm(ComparisonOperator.GREATER_EQ, left, right);
	}

	public static RealToIntTerm asInt(Term<Double> body) {
		return new RealToIntTerm(body);
	}
}
