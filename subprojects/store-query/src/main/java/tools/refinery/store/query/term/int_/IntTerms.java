/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.term.Aggregator;
import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.ExtremeValueAggregator;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.comparable.*;

import java.util.Comparator;

public final class IntTerms {
	public static final Aggregator<Integer, Integer> INT_SUM = IntSumAggregator.INSTANCE;
	public static final Aggregator<Integer, Integer> INT_MIN = new ExtremeValueAggregator<>(Integer.class,
			Integer.MAX_VALUE);
	public static final Aggregator<Integer, Integer> INT_MAX = new ExtremeValueAggregator<>(Integer.class,
			Integer.MIN_VALUE, Comparator.reverseOrder());

	private IntTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static Term<Integer> constant(Integer value) {
		return new ConstantTerm<>(Integer.class, value);
	}

	public static Term<Integer> plus(Term<Integer> body) {
		return new IntPlusTerm(body);
	}

	public static Term<Integer> minus(Term<Integer> body) {
		return new IntMinusTerm(body);
	}

	public static Term<Integer> add(Term<Integer> left, Term<Integer> right) {
		return new IntAddTerm(left, right);
	}

	public static Term<Integer> sub(Term<Integer> left, Term<Integer> right) {
		return new IntSubTerm(left, right);
	}

	public static Term<Integer> mul(Term<Integer> left, Term<Integer> right) {
		return new IntMulTerm(left, right);
	}

	public static Term<Integer> div(Term<Integer> left, Term<Integer> right) {
		return new IntDivTerm(left, right);
	}

	public static Term<Integer> pow(Term<Integer> left, Term<Integer> right) {
		return new IntPowTerm(left, right);
	}

	public static Term<Integer> min(Term<Integer> left, Term<Integer> right) {
		return new IntMinTerm(left, right);
	}

	public static Term<Integer> max(Term<Integer> left, Term<Integer> right) {
		return new IntMaxTerm(left, right);
	}

	public static Term<Boolean> eq(Term<Integer> left, Term<Integer> right) {
		return new EqTerm<>(Integer.class, left, right);
	}

	public static Term<Boolean> notEq(Term<Integer> left, Term<Integer> right) {
		return new NotEqTerm<>(Integer.class, left, right);
	}

	public static Term<Boolean> less(Term<Integer> left, Term<Integer> right) {
		return new LessTerm<>(Integer.class, left, right);
	}

	public static Term<Boolean> lessEq(Term<Integer> left, Term<Integer> right) {
		return new LessEqTerm<>(Integer.class, left, right);
	}

	public static Term<Boolean> greater(Term<Integer> left, Term<Integer> right) {
		return new GreaterTerm<>(Integer.class, left, right);
	}

	public static Term<Boolean> greaterEq(Term<Integer> left, Term<Integer> right) {
		return new GreaterEqTerm<>(Integer.class, left, right);
	}

	public static Term<Integer> asInt(Term<Double> body) {
		return new RealToIntTerm(body);
	}
}
