/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.truthvalue;

import tools.refinery.logic.ComparableAbstractDomain;

// Singleton pattern, because there is only one domain for truth values.
@SuppressWarnings("squid:S6548")
public final class TruthValueDomain implements ComparableAbstractDomain<TruthValue, Boolean> {
	public static final TruthValueDomain INSTANCE = new TruthValueDomain();

	private TruthValueDomain() {
	}

	@Override
	public Class<TruthValue> abstractType() {
		return TruthValue.class;
	}

	@Override
	public Class<Boolean> concreteType() {
		return Boolean.class;
	}

	@Override
	public TruthValue unknown() {
		return TruthValue.UNKNOWN;
	}

	@Override
	public TruthValue error() {
		return TruthValue.ERROR;
	}

	@Override
	public TruthValue toAbstract(Boolean concreteValue) {
		return TruthValue.of(concreteValue);
	}

	@Override
	public TruthValue negativeInfinity() {
		return TruthValue.FALSE;
	}

	@Override
	public TruthValue positiveInfinity() {
		return TruthValue.TRUE;
	}
}
