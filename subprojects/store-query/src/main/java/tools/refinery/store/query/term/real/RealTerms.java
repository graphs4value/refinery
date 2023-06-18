/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.real;

import tools.refinery.store.query.term.Aggregator;
import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.ExtremeValueAggregator;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.comparable.*;

import java.util.Comparator;

public final class RealTerms {
	public static final Aggregator<Double, Double> REAL_SUM = RealSumAggregator.INSTANCE;
	public static final Aggregator<Double, Double> REAL_MIN = new ExtremeValueAggregator<>(Double.class,
			Double.POSITIVE_INFINITY);
	public static final Aggregator<Double, Double> REAL_MAX = new ExtremeValueAggregator<>(Double.class,
			Double.NEGATIVE_INFINITY, Comparator.reverseOrder());

	private RealTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static Term<Double> constant(Double value) {
		return new ConstantTerm<>(Double.class, value);
	}

	public static Term<Double> plus(Term<Double> body) {
		return new RealPlusTerm(body);
	}

	public static Term<Double> minus(Term<Double> body) {
		return new RealMinusTerm(body);
	}

	public static Term<Double> add(Term<Double> left, Term<Double> right) {
		return new RealAddTerm(left, right);
	}

	public static Term<Double> sub(Term<Double> left, Term<Double> right) {
		return new RealSubTerm(left, right);
	}

	public static Term<Double> mul(Term<Double> left, Term<Double> right) {
		return new RealMulTerm(left, right);
	}

	public static Term<Double> div(Term<Double> left, Term<Double> right) {
		return new RealDivTerm(left, right);
	}

	public static Term<Double> pow(Term<Double> left, Term<Double> right) {
		return new RealPowTerm(left, right);
	}

	public static Term<Double> min(Term<Double> left, Term<Double> right) {
		return new RealMinTerm(left, right);
	}

	public static Term<Double> max(Term<Double> left, Term<Double> right) {
		return new RealMaxTerm(left, right);
	}

	public static Term<Boolean> eq(Term<Double> left, Term<Double> right) {
		return new EqTerm<>(Double.class, left, right);
	}

	public static Term<Boolean> notEq(Term<Double> left, Term<Double> right) {
		return new NotEqTerm<>(Double.class, left, right);
	}

	public static Term<Boolean> less(Term<Double> left, Term<Double> right) {
		return new LessTerm<>(Double.class, left, right);
	}

	public static Term<Boolean> lessEq(Term<Double> left, Term<Double> right) {
		return new LessEqTerm<>(Double.class, left, right);
	}

	public static Term<Boolean> greater(Term<Double> left, Term<Double> right) {
		return new GreaterTerm<>(Double.class, left, right);
	}

	public static Term<Boolean> greaterEq(Term<Double> left, Term<Double> right) {
		return new GreaterEqTerm<>(Double.class, left, right);
	}

	public static Term<Double> asReal(Term<Integer> body) {
		return new IntToRealTerm(body);
	}
}
