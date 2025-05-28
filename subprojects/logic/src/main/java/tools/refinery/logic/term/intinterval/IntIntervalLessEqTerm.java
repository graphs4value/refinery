package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

public class IntIntervalLessEqTerm extends BinaryTerm<TruthValue, IntInterval, IntInterval> {
	public IntIntervalLessEqTerm(Term<IntInterval> left, Term<IntInterval> right) {
		super(TruthValue.class, IntInterval.class, IntInterval.class, left, right);
	}

	@Override
	protected TruthValue doEvaluate(IntInterval leftValue, IntInterval rightValue) {
		var must = leftValue.upperBound().lessThanOrEquals(rightValue.lowerBound());
		var may = leftValue.lowerBound().lessThanOrEquals(rightValue.upperBound());
		return TruthValue.of(may,must);
	}

	@Override
	protected Term<TruthValue> constructWithSubTerms(Term<IntInterval> newLeft, Term<IntInterval> newRight) {
		return new IntIntervalLessEqTerm(newLeft, newRight);
	}
}
