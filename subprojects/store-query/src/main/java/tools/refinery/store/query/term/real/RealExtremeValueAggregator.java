package tools.refinery.store.query.term.real;

import tools.refinery.store.query.term.ExtremeValueAggregator;

import java.util.Comparator;

public final class RealExtremeValueAggregator {
	public static final ExtremeValueAggregator<Double> MINIMUM = new ExtremeValueAggregator<>(Double.class,
			Double.POSITIVE_INFINITY);

	public static final ExtremeValueAggregator<Double> MAXIMUM = new ExtremeValueAggregator<>(Double.class,
			Double.NEGATIVE_INFINITY, Comparator.reverseOrder());

	private RealExtremeValueAggregator() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}
}
