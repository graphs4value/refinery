package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

public class IntIntervalInTerm extends BinaryTerm<TruthValue, IntInterval, IntInterval> {
	public IntIntervalInTerm(Term<IntInterval> left, Term<IntInterval> right) {
		super(TruthValue.class, IntInterval.class, IntInterval.class, left, right);
	}

	@Override
	protected TruthValue doEvaluate(IntInterval leftValue, IntInterval rightValue) {
		var must = leftValue.lowerBound().greaterThanOrEquals(rightValue.lowerBound()) &&
				leftValue.lowerBound().lessThanOrEquals(rightValue.upperBound()) &&
				leftValue.upperBound().lessThanOrEquals(rightValue.upperBound()) &&
				leftValue.upperBound().greaterThanOrEquals(rightValue.lowerBound());

		var may = !((leftValue.lowerBound().greaterThanOrEquals(rightValue.upperBound()) &&
				leftValue.upperBound().greaterThanOrEquals(rightValue.upperBound())) ||
				(leftValue.lowerBound().lessThanOrEquals(rightValue.lowerBound()) &&
				leftValue.upperBound().lessThanOrEquals(rightValue.lowerBound())));

		return TruthValue.of(may,must);
	}

	@Override
	protected Term<TruthValue> constructWithSubTerms(Term<IntInterval> newLeft, Term<IntInterval> newRight) {
		return new IntIntervalInTerm(newLeft, newRight);
	}
}
