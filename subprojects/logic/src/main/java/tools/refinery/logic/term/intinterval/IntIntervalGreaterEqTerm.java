package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

public class IntIntervalGreaterEqTerm extends BinaryTerm<TruthValue, IntInterval, IntInterval> {
	protected IntIntervalGreaterEqTerm(Term<IntInterval> left, Term<IntInterval> right) {
		super(TruthValue.class, IntInterval.class, IntInterval.class, left, right);
	}

	@Override
	protected TruthValue doEvaluate(IntInterval leftValue, IntInterval rightValue) {
		boolean may = leftValue.upperBound().greaterThanOrEquals(rightValue.lowerBound());
		boolean must = leftValue.lowerBound().greaterThanOrEquals(rightValue.upperBound());
		return TruthValue.of(may, must);
	}

	@Override
	protected Term<TruthValue> constructWithSubTerms(Term<IntInterval> newLeft, Term<IntInterval> newRight) {
		return new IntIntervalGreaterEqTerm(newLeft, newRight);
	}
}
