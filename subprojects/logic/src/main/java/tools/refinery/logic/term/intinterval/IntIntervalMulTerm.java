package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;

public class IntIntervalMulTerm extends BinaryTerm<IntInterval, IntInterval, IntInterval> {
	public IntIntervalMulTerm(Term<IntInterval> left, Term<IntInterval> right) {
		super(IntInterval.class, IntInterval.class, IntInterval.class, left, right);
	}

	@Override
	protected Term<IntInterval> constructWithSubTerms(Term<IntInterval> newLeft,
												 Term<IntInterval> newRight) {
		return new IntIntervalMulTerm(newLeft, newRight);
	}

	@Override
	protected IntInterval doEvaluate(IntInterval leftValue, IntInterval rightValue) {
		return leftValue.mul(rightValue);
	}

	@Override
	public String toString() {
		return "(%s + %s)".formatted(getLeft(), getRight());
	}
}
