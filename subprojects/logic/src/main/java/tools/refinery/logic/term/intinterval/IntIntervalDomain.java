package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.AbstractDomain;

// Singleton pattern, because there is only one domain for integer intervals.
@SuppressWarnings("squid:S6548")
public class IntIntervalDomain implements AbstractDomain<IntInterval, Integer> {
	public static final IntIntervalDomain INSTANCE = new IntIntervalDomain();

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
}
