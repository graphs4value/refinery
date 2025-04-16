package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

public final class IntIntervalTerms {
	private IntIntervalTerms() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static Term<TruthValue> less(Term<IntInterval> left, Term<IntInterval> right) {
		return new IntIntervalLessTerm(left, right);
	}

	public static Term<TruthValue> lessEq(Term<IntInterval> left, Term<IntInterval> right) {
		return new IntIntervalLessEqTerm(left, right);
	}

	public static Term<TruthValue> greater(Term<IntInterval> left, Term<IntInterval> right) {
		return new IntIntervalGreaterTerm(left, right);
	}

	public static Term<TruthValue> greaterEq(Term<IntInterval> left, Term<IntInterval> right) {
		return new IntIntervalGreaterEqTerm(left, right);
	}

	public static Term<TruthValue> in(Term<IntInterval> left, Term<IntInterval> right) {
		return new IntIntervalInTerm(left, right);
	}

	public static Term<TruthValue> equals(Term<IntInterval> left, Term<IntInterval> right) {
		return new IntIntervalEqTerm(left, right);
	}

	public static Term<IntInterval> constant(IntInterval value) {
		return new ConstantTerm<>(IntInterval.class, value);
	}
}
