/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.realinterval;

import tools.refinery.logic.ComparableAbstractDomain;

import java.math.BigDecimal;

// Singleton pattern, because there is only one domain for integer intervals.
@SuppressWarnings("squid:S6548")
public class RealIntervalDomain implements ComparableAbstractDomain<RealInterval, BigDecimal> {
	public static final RealIntervalDomain INSTANCE = new RealIntervalDomain();

	private RealIntervalDomain() {
	}

	@Override
	public Class<RealInterval> abstractType() {
		return RealInterval.class;
	}

	@Override
	public Class<BigDecimal> concreteType() {
		return BigDecimal.class;
	}

	@Override
	public RealInterval unknown() {
		return RealInterval.UNKNOWN;
	}

	@Override
	public RealInterval error() {
		return RealInterval.ERROR;
	}

	@Override
	public RealInterval toAbstract(BigDecimal concreteValue) {
		return RealInterval.of(concreteValue);
	}

	@Override
	public RealInterval negativeInfinity() {
		return RealInterval.NEGATIVE_INFINITY;
	}

	@Override
	public RealInterval positiveInfinity() {
		return RealInterval.POSITIVE_INFINITY;
	}
}
