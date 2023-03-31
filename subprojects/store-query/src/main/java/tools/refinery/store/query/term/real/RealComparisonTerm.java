package tools.refinery.store.query.term.real;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.ComparisonOperator;
import tools.refinery.store.query.term.ComparisonTerm;
import tools.refinery.store.query.term.Term;

public class RealComparisonTerm extends ComparisonTerm<Double> {
	public RealComparisonTerm(ComparisonOperator operator, Term<Double> left, Term<Double> right) {
		super(operator, left, right);
	}

	@Override
	public Class<Double> getOperandType() {
		return Double.class;
	}

	@Override
	public Term<Boolean> doSubstitute(Substitution substitution, Term<Double> substitutedLeft,
									  Term<Double> substitutedRight) {
		return new RealComparisonTerm(getOperator(), substitutedLeft, substitutedRight);
	}

	@Override
	protected Boolean doEvaluate(Double leftValue, Double rightValue) {
		return switch (getOperator()) {
			case EQ -> leftValue.equals(rightValue);
			case NOT_EQ -> !leftValue.equals(rightValue);
			case LESS -> leftValue < rightValue;
			case LESS_EQ -> leftValue <= rightValue;
			case GREATER -> leftValue > rightValue;
			case GREATER_EQ -> leftValue >= rightValue;
		};
	}
}
