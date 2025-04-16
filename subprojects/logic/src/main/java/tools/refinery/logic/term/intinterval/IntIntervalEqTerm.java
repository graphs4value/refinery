package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

class IntIntervalEqTerm extends BinaryTerm<TruthValue, IntInterval, IntInterval> {
	protected IntIntervalEqTerm(Term<IntInterval> left, Term<IntInterval> right) {
		super(TruthValue.class, IntInterval.class, IntInterval.class, left, right);
	}

	@Override
	protected TruthValue doEvaluate(IntInterval leftValue, IntInterval rightValue) {
		var may = (leftValue.lowerBound().greaterThanOrEquals(rightValue.lowerBound()) &&
				leftValue.lowerBound().lessThanOrEquals(rightValue.upperBound())) || (
				leftValue.upperBound().lessThanOrEquals(rightValue.upperBound()) &&
				leftValue.upperBound().greaterThanOrEquals(rightValue.lowerBound()));
		var must = leftValue.isConcrete() && leftValue.equals(rightValue);
		return TruthValue.of(may,must);
	}

	@Override
	protected Term<TruthValue> constructWithSubTerms(Term<IntInterval> newLeft, Term<IntInterval> newRight) {
		return new IntIntervalEqTerm(newLeft, newRight);
	}
}
