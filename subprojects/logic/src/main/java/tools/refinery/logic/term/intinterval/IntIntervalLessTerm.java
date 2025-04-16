package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

class IntIntervalLessTerm extends BinaryTerm<TruthValue, IntInterval, IntInterval> {
	protected IntIntervalLessTerm(Term<IntInterval> left, Term<IntInterval> right) {
		super(TruthValue.class, IntInterval.class, IntInterval.class, left, right);
	}

	@Override
	protected TruthValue doEvaluate(IntInterval leftValue, IntInterval rightValue) {
		var may = !leftValue.lowerBound().greaterThanOrEquals(rightValue.upperBound());
		var must = !leftValue.lowerBound().greaterThanOrEquals(rightValue.lowerBound());
		return TruthValue.of(may,must);
	}

	@Override
	protected Term<TruthValue> constructWithSubTerms(Term<IntInterval> newLeft, Term<IntInterval> newRight) {
		return new IntIntervalLessTerm(newLeft, newRight);
	}
}
