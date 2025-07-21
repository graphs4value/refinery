/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.intinterval.IntInterval;

import java.util.function.BiFunction;

public sealed abstract class PartialAggregator<
		A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2> implements AnyPartialAggregator {
	private final AbstractDomain<A2, C2> bodyDomain;
	private final AbstractDomain<A, C> resultDomain;

	protected PartialAggregator(AbstractDomain<A, C> resultDomain, AbstractDomain<A2, C2> bodyDomain) {
		this.bodyDomain = bodyDomain;
		this.resultDomain = resultDomain;
	}

	@Override
	public AbstractDomain<A2, C2> getBodyDomain() {
		return bodyDomain;
	}

	@Override
	public AbstractDomain<A, C> getResultDomain() {
		return resultDomain;
	}

	public static final class MultiplicitySensitive<
			A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2, T>
			extends PartialAggregator<A, C, A2, C2> {
		private final Class<T> intermediateType;
		private final BiFunction<Term<IntInterval>, Term<A2>, Term<T>> withWeight;
		private final Aggregator<A, T> innerAggregator;

		private MultiplicitySensitive(
				AbstractDomain<A, C> resultDomain, AbstractDomain<A2, C2> bodyDomain, Class<T> intermediateType,
				BiFunction<Term<IntInterval>, Term<A2>, Term<T>> withWeight, Aggregator<A, T> innerAggregator) {
			super(resultDomain, bodyDomain);
			this.intermediateType = intermediateType;
			this.withWeight = withWeight;
			this.innerAggregator = innerAggregator;
		}

		public Class<T> getIntermediateType() {
			return intermediateType;
		}

		public Term<T> withWeight(Term<IntInterval> left, Term<A2> right) {
			return withWeight.apply(left, right);
		}

		public Aggregator<A, T> getInnerAggregator() {
			return innerAggregator;
		}
	}

	public static final class MultiplicityInsensitive<
			A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2>
			extends PartialAggregator<A, C, A2, C2> {
		private final A2 neutralElement;
		private final Aggregator<A, A2> innerAggregator;

		private MultiplicityInsensitive(
				AbstractDomain<A, C> resultDomain, AbstractDomain<A2, C2> bodyDomain, A2 neutralElement,
				Aggregator<A, A2> innerAggregator) {
			super(resultDomain, bodyDomain);
			this.neutralElement = neutralElement;
			this.innerAggregator = innerAggregator;
		}

		public A2 getNeutralElement() {
			return neutralElement;
		}

		public Aggregator<A, A2> getInnerAggregator() {
			return innerAggregator;
		}
	}

	public static <A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2, T>
	PartialAggregator<A, C, A2, C2> multiplicitySensitive(
			AbstractDomain<A, C> resultDomain, AbstractDomain<A2, C2> bodyDomain, Class<T> intermediateType,
			BiFunction<Term<IntInterval>, Term<A2>, Term<T>> withWeight, Aggregator<A, T> innerAggregator) {
		return new MultiplicitySensitive<>(resultDomain, bodyDomain, intermediateType, withWeight, innerAggregator);
	}

	public static <A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2>
	PartialAggregator<A, C, A2, C2> multiplicitySensitive(
			AbstractDomain<A, C> resultDomain, AbstractDomain<A2, C2> bodyDomain,
			BiFunction<Term<IntInterval>, Term<A2>, Term<A2>> withWeight, Aggregator<A, A2> innerAggregator) {
		return multiplicitySensitive(resultDomain, bodyDomain, bodyDomain.abstractType(), withWeight, innerAggregator);
	}

	public static <A extends AbstractValue<A, C>, C, T>
	PartialAggregator<A, C, A, C> multiplicitySensitive(
			AbstractDomain<A, C> domain, Class<T> intermediateType,
			BiFunction<Term<IntInterval>, Term<A>, Term<T>> withWeight, Aggregator<A, T> innerAggregator) {
		return multiplicitySensitive(domain, domain, intermediateType, withWeight, innerAggregator);
	}

	public static <A extends AbstractValue<A, C>, C>
	PartialAggregator<A, C, A, C> multiplicitySensitive(
			AbstractDomain<A, C> domain, BiFunction<Term<IntInterval>, Term<A>, Term<A>> withWeight,
			Aggregator<A, A> innerAggregator) {
		return multiplicitySensitive(domain, domain.abstractType(), withWeight, innerAggregator);
	}

	public static <A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2>
	PartialAggregator<A, C, A2, C2> multiplicityInsensitive(
			AbstractDomain<A, C> resultDomain, AbstractDomain<A2, C2> bodyDomain, A2 neutralElement,
			Aggregator<A, A2> innerAggregator) {
		return new MultiplicityInsensitive<>(resultDomain, bodyDomain, neutralElement, innerAggregator);
	}

	public static <A extends AbstractValue<A, C>, C>
	PartialAggregator<A, C, A, C> multiplicityInsensitive(
			AbstractDomain<A, C> domain, Aggregator<A, A> innerAggregator) {
		return multiplicityInsensitive(domain, domain, innerAggregator.getEmptyResult(), innerAggregator);
	}
}
