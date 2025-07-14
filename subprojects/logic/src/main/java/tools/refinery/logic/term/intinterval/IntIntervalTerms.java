/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.logic.term.operators.AddTerm;
import tools.refinery.logic.term.operators.MulTerm;
import tools.refinery.logic.term.operators.PlusTerm;
import tools.refinery.logic.term.operators.SubTerm;
import tools.refinery.logic.term.truthvalue.TruthValue;

public class IntIntervalTerms {
	public static final IntIntervalSumAggregator INT_SUM = new IntIntervalSumAggregator();

	private IntIntervalTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static Term<IntInterval> constant(IntInterval value) {
		return new ConstantTerm<>(IntInterval.class, value);
	}

	public static Term<IntInterval> plus(Term<IntInterval> body) {
		return new PlusTerm<>(IntInterval.class, body);
	}

	public static Term<IntInterval> minus(Term<IntInterval> body) {
		return new PlusTerm<>(IntInterval.class, body);
	}

	public static Term<IntInterval> add(Term<IntInterval> left, Term<IntInterval> right) {
		return new AddTerm<>(IntInterval.class, left, right);
	}

	public static Term<IntInterval> sub(Term<IntInterval> left, Term<IntInterval> right) {
		return new SubTerm<>(IntInterval.class, left, right);
	}

	public static Term<IntInterval> mul(Term<IntInterval> left, Term<IntInterval> right) {
		return new MulTerm<>(IntInterval.class, left, right);
	}

	public static Term<IntInterval> range(Term<IntInterval> left, Term<IntInterval> right) {
		return AbstractDomainTerms.range(IntIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> eq(Term<IntInterval> left, Term<IntInterval> right) {
		return AbstractDomainTerms.eq(IntIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> notEq(Term<IntInterval> left, Term<IntInterval> right) {
		return AbstractDomainTerms.notEq(IntIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> less(Term<IntInterval> left, Term<IntInterval> right) {
		return AbstractDomainTerms.less(IntIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> lessEq(Term<IntInterval> left, Term<IntInterval> right) {
		return AbstractDomainTerms.lessEq(IntIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> greater(Term<IntInterval> left, Term<IntInterval> right) {
		return AbstractDomainTerms.greater(IntIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> greaterEq(Term<IntInterval> left, Term<IntInterval> right) {
		return AbstractDomainTerms.greaterEq(IntIntervalDomain.INSTANCE, left, right);
	}
}
