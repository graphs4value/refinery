/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.uppercardinality;

import tools.refinery.store.query.term.Aggregator;
import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.ExtremeValueAggregator;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.comparable.*;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import java.util.Comparator;

public final class UpperCardinalityTerms {
	public static final Aggregator<UpperCardinality, UpperCardinality> UPPER_CARDINALITY_SUM =
			UpperCardinalitySumAggregator.INSTANCE;
	public static final Aggregator<UpperCardinality, UpperCardinality> UPPER_CARDINALITY_MIN =
			new ExtremeValueAggregator<>(UpperCardinality.class, UpperCardinalities.UNBOUNDED);
	public static final Aggregator<UpperCardinality, UpperCardinality> UPPER_CARDINALITY_MAX =
			new ExtremeValueAggregator<>(UpperCardinality.class, UpperCardinalities.ZERO, Comparator.reverseOrder());

	private UpperCardinalityTerms() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static Term<UpperCardinality> constant(UpperCardinality value) {
		return new ConstantTerm<>(UpperCardinality.class, value);
	}

	public static Term<UpperCardinality> add(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		return new UpperCardinalityAddTerm(left, right);
	}

	public static Term<UpperCardinality> mul(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		return new UpperCardinalityMulTerm(left, right);
	}

	public static Term<UpperCardinality> min(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		return new UpperCardinalityMinTerm(left, right);
	}

	public static Term<UpperCardinality> max(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		return new UpperCardinalityMaxTerm(left, right);
	}

	public static Term<Boolean> eq(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		return new EqTerm<>(UpperCardinality.class, left, right);
	}

	public static Term<Boolean> notEq(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		return new NotEqTerm<>(UpperCardinality.class, left, right);
	}

	public static Term<Boolean> less(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		return new LessTerm<>(UpperCardinality.class, left, right);
	}

	public static Term<Boolean> lessEq(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		return new LessEqTerm<>(UpperCardinality.class, left, right);
	}

	public static Term<Boolean> greater(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		return new GreaterTerm<>(UpperCardinality.class, left, right);
	}

	public static Term<Boolean> greaterEq(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		return new GreaterEqTerm<>(UpperCardinality.class, left, right);
	}
}
