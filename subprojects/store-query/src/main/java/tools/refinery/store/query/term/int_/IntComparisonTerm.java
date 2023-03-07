package tools.refinery.store.query.term.int_;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.ComparisonOperator;
import tools.refinery.store.query.term.ComparisonTerm;
import tools.refinery.store.query.term.Term;

public class IntComparisonTerm extends ComparisonTerm<Integer> {
	public IntComparisonTerm(ComparisonOperator operator, Term<Integer> left, Term<Integer> right) {
		super(operator, left, right);
	}

	@Override
	public Class<Integer> getOperandType() {
		return Integer.class;
	}

	@Override
	public Term<Boolean> doSubstitute(Substitution substitution, Term<Integer> substitutedLeft,
									  Term<Integer> substitutedRight) {
		return new IntComparisonTerm(getOperator(), substitutedLeft, substitutedRight);
	}

	@Override
	protected Boolean doEvaluate(Integer leftValue, Integer rightValue) {
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
