package tools.refinery.store.query.term.real;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.ArithmeticBinaryOperator;
import tools.refinery.store.query.term.ArithmeticBinaryTerm;
import tools.refinery.store.query.term.Term;

public class RealArithmeticBinaryTerm extends ArithmeticBinaryTerm<Double> {
	public RealArithmeticBinaryTerm(ArithmeticBinaryOperator operator, Term<Double> left, Term<Double> right) {
		super(operator, left, right);
	}

	@Override
	public Class<Double> getType() {
		return Double.class;
	}

	@Override
	public Term<Double> doSubstitute(Substitution substitution, Term<Double> substitutedLeft,
									 Term<Double> substitutedRight) {
		return new RealArithmeticBinaryTerm(getOperator(), substitutedLeft, substitutedRight);
	}

	@Override
	protected Double doEvaluate(Double leftValue, Double rightValue) {
		return switch (getOperator()) {
			case ADD -> leftValue + rightValue;
			case SUB -> leftValue - rightValue;
			case MUL -> leftValue * rightValue;
			case DIV -> leftValue / rightValue;
			case POW -> Math.pow(leftValue, rightValue);
			case MIN -> Math.min(leftValue, rightValue);
			case MAX -> Math.max(leftValue, rightValue);
		};
	}
}
