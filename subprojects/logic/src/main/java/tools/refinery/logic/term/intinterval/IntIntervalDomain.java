/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.ComparableAbstractDomain;

// Singleton pattern, because there is only one domain for integer intervals.
@SuppressWarnings("squid:S6548")
public class IntIntervalDomain implements ComparableAbstractDomain<IntInterval, Integer> {
	public static final IntIntervalDomain INSTANCE = new IntIntervalDomain();

	private IntIntervalDomain() {
	}

	@Override
	public Class<IntInterval> abstractType() {
		return IntInterval.class;
	}

	@Override
	public Class<Integer> concreteType() {
		return Integer.class;
	}

	@Override
	public IntInterval unknown() {
		return IntInterval.UNKNOWN;
	}

	@Override
	public IntInterval error() {
		return IntInterval.ERROR;
	}

	@Override
	public IntInterval toAbstract(Integer concreteValue) {
		return IntInterval.of(concreteValue);
	}

	@Override
	public IntInterval negativeInfinity() {
		return IntInterval.NEGATIVE_INFINITY;
	}

	@Override
	public IntInterval positiveInfinity() {
		return IntInterval.POSITIVE_INFINITY;
	}
}
