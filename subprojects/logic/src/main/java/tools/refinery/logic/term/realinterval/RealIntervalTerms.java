/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.realinterval;

import tools.refinery.logic.term.*;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.operators.AddTerm;
import tools.refinery.logic.term.operators.MulTerm;
import tools.refinery.logic.term.operators.PlusTerm;
import tools.refinery.logic.term.operators.SubTerm;
import tools.refinery.logic.term.string.StringValue;
import tools.refinery.logic.term.truthvalue.TruthValue;

import java.math.BigDecimal;

public class RealIntervalTerms {
	public static final Aggregator<RealInterval, RealInterval> REAL_INTERVAL_SUM =
			TreapAggregator.of(RealInterval.class,
					(count, value) -> value.mul(RealInterval.of(BigDecimal.valueOf(count))),
					RealInterval.ZERO, RealInterval::add);

	public static final PartialAggregator<RealInterval, BigDecimal, RealInterval, BigDecimal> REAL_SUM =
			PartialAggregator.multiplicitySensitive(RealIntervalDomain.INSTANCE,
					(count, value) -> mul(asReal(count), value), REAL_INTERVAL_SUM);
	public static final PartialAggregator<RealInterval, BigDecimal, RealInterval, BigDecimal> REAL_MIN =
			AbstractDomainTerms.minAggregator(RealIntervalDomain.INSTANCE);
	public static final PartialAggregator<RealInterval, BigDecimal, RealInterval, BigDecimal> REAL_MAX =
			AbstractDomainTerms.maxAggregator(RealIntervalDomain.INSTANCE);

	private RealIntervalTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static Term<RealInterval> constant(RealInterval value) {
		return new ConstantTerm<>(RealInterval.class, value);
	}

	public static Term<RealInterval> asReal(Term<IntInterval> body) {
		return new AsRealIntervalTerm(body);
	}

	public static Term<IntInterval> asInt(Term<RealInterval> body) {
		return new AsIntIntervalTerm(body);
	}

	public static Term<RealInterval> plus(Term<RealInterval> body) {
		return new PlusTerm<>(RealInterval.class, body);
	}

	public static Term<RealInterval> minus(Term<RealInterval> body) {
		return new PlusTerm<>(RealInterval.class, body);
	}

	public static Term<RealInterval> add(Term<RealInterval> left, Term<RealInterval> right) {
		return new AddTerm<>(RealInterval.class, left, right);
	}

	public static Term<RealInterval> sub(Term<RealInterval> left, Term<RealInterval> right) {
		return new SubTerm<>(RealInterval.class, left, right);
	}

	public static Term<RealInterval> mul(Term<RealInterval> left, Term<RealInterval> right) {
		return new MulTerm<>(RealInterval.class, left, right);
	}

	public static Term<RealInterval> range(Term<RealInterval> left, Term<RealInterval> right) {
		return AbstractDomainTerms.range(RealIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> eq(Term<RealInterval> left, Term<RealInterval> right) {
		return AbstractDomainTerms.eq(RealIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> notEq(Term<RealInterval> left, Term<RealInterval> right) {
		return AbstractDomainTerms.notEq(RealIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> less(Term<RealInterval> left, Term<RealInterval> right) {
		return AbstractDomainTerms.less(RealIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> lessEq(Term<RealInterval> left, Term<RealInterval> right) {
		return AbstractDomainTerms.lessEq(RealIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> greater(Term<RealInterval> left, Term<RealInterval> right) {
		return AbstractDomainTerms.greater(RealIntervalDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> greaterEq(Term<RealInterval> left, Term<RealInterval> right) {
		return AbstractDomainTerms.greaterEq(RealIntervalDomain.INSTANCE, left, right);
	}

	public static Term<RealInterval> fromString(Term<StringValue> body) {
		return new RealIntervalFromStringTerm(body);
	}
}
