package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.ArithmeticBinaryOperator;
import tools.refinery.store.query.term.ArithmeticBinaryTerm;
import tools.refinery.store.query.term.Term;

public class IntArithmeticBinaryTerm extends ArithmeticBinaryTerm<Integer> {
	public IntArithmeticBinaryTerm(ArithmeticBinaryOperator operator, Term<Integer> left, Term<Integer> right) {
		super(operator, left, right);
	}

	@Override
	public Class<Integer> getType() {
		return Integer.class;
	}

	@Override
	public Term<Integer> doSubstitute(Substitution substitution, Term<Integer> substitutedLeft,
									  Term<Integer> substitutedRight) {
		return new IntArithmeticBinaryTerm(getOperator(), substitutedLeft, substitutedRight);
	}

	@Override
	protected Integer doEvaluate(Integer leftValue, Integer rightValue) {
		return switch (getOperator()) {
			case ADD -> leftValue + rightValue;
			case SUB -> leftValue - rightValue;
			case MUL -> leftValue * rightValue;
			case DIV -> rightValue == 0 ? null : leftValue / rightValue;
			case POW -> rightValue < 0 ? null : power(leftValue, rightValue);
			case MIN -> Math.min(leftValue, rightValue);
			case MAX -> Math.max(leftValue, rightValue);
		};
	}

	private static int power(int base, int exponent) {
		int accum = 1;
		while (exponent > 0) {
			if (exponent % 2 == 1) {
				accum = accum * base;
			}
			base = base * base;
			exponent = exponent / 2;
		}
		return accum;
	}
}
