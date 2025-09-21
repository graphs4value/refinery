/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.ComparableAbstractDomain;
import tools.refinery.logic.term.*;
import tools.refinery.logic.term.string.StringValue;
import tools.refinery.logic.term.truthvalue.TruthValue;

public final class AbstractDomainTerms {
	private AbstractDomainTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> PartialAggregator<A, C, A, C>
	minAggregator(ComparableAbstractDomain<A, C> domain) {
		var innerAggregator = TreapAggregator.of(domain.abstractType(), (ignored, value) -> value,
				domain.positiveInfinity(), ComparableAbstractValue::min);
		return PartialAggregator.multiplicityInsensitive(domain, innerAggregator);
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> PartialAggregator<A, C, A, C>
	maxAggregator(ComparableAbstractDomain<A, C> domain) {
		var innerAggregator = TreapAggregator.of(domain.abstractType(), (ignored, value) -> value,
				domain.negativeInfinity(), ComparableAbstractValue::max);
		return PartialAggregator.multiplicityInsensitive(domain, innerAggregator);
	}

	public static <A extends AbstractValue<A, C>, C> Term<A> unknown(AbstractDomain<A, C> abstractDomain) {
		return new ConstantTerm<>(abstractDomain.abstractType(), abstractDomain.unknown());
	}

	public static <A extends AbstractValue<A, C>, C> Term<A> error(AbstractDomain<A, C> abstractDomain) {
		return new ConstantTerm<>(abstractDomain.abstractType(), abstractDomain.error());
	}

	public static <A extends AbstractValue<A, C>, C> Term<TruthValue> eq(
			AbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainEqTerm<>(abstractDomain, left, right);
	}

	public static <A extends AbstractValue<A, C>, C> Term<TruthValue> notEq(
			AbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainNotEqTerm<>(abstractDomain, left, right);
	}

	public static <A extends AbstractValue<A, C>, C> Term<Boolean> subset(
			AbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainSubsetTerm<>(abstractDomain, left, right);
	}

	public static <A extends AbstractValue<A, C>, C> Term<Boolean> superset(
			AbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainSupersetTerm<>(abstractDomain, left, right);
	}

	public static <A extends AbstractValue<A, C>, C> Term<A> join(
			AbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainJoinTerm<>(abstractDomain, left, right);
	}

	public static <A extends AbstractValue<A, C>, C> Term<A> meet(
			AbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainMeetTerm<>(abstractDomain, left, right);
	}

	public static <A extends AbstractValue<A, C>, C> Term<Boolean> isError(AbstractDomain<A, C> abstractDomain,
																		   Term<A> body) {
		return new IsErrorTerm<>(abstractDomain, body);
	}

	public static <A extends AbstractValue<A, C>, C> Term<Boolean> isConcrete(AbstractDomain<A, C> abstractDomain,
																			  Term<A> body) {
		return new IsConcreteTerm<>(abstractDomain, body);
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<A> negativeInfinity(
			ComparableAbstractDomain<A, C> abstractDomain) {
		return new ConstantTerm<>(abstractDomain.abstractType(), abstractDomain.negativeInfinity());
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<A> positiveInfinity(
			ComparableAbstractDomain<A, C> abstractDomain) {
		return new ConstantTerm<>(abstractDomain.abstractType(), abstractDomain.positiveInfinity());
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<TruthValue> less(
			ComparableAbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainLessTerm<>(abstractDomain, left, right);
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<TruthValue> lessEq(
			ComparableAbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainLessEqTerm<>(abstractDomain, left, right);
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<TruthValue> greater(
			ComparableAbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainGreaterTerm<>(abstractDomain, left, right);
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<TruthValue> greaterEq(
			ComparableAbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainGreaterEqTerm<>(abstractDomain, left, right);
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<A> range(
			ComparableAbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainRangeTerm<>(abstractDomain, left, right);
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<A> min(
			ComparableAbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainMinTerm<>(abstractDomain, left, right);
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<A> max(
			ComparableAbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		return new AbstractDomainMaxTerm<>(abstractDomain, left, right);
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<A> lowerBound(
			ComparableAbstractDomain<A, C> abstractDomain, Term<A> body) {
		return new AbstractDomainLowerBoundTerm<>(abstractDomain, body);
	}

	public static <A extends ComparableAbstractValue<A, C>, C extends Comparable<C>> Term<A> upperBound(
			ComparableAbstractDomain<A, C> abstractDomain, Term<A> body) {
		return new AbstractDomainUpperBoundTerm<>(abstractDomain, body);
	}

	public static <A extends AbstractValue<A, C>, C> Term<StringValue> asString(
			AbstractDomain<A, C> abstractDomain, Term<A> body) {
		return new AsStringTerm<>(abstractDomain, body);
	}
}
