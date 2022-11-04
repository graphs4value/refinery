package tools.refinery.store.query.atom;

public sealed interface CallPolarity permits SimplePolarity, CountingPolarity {
	boolean isPositive();

	boolean isTransitive();

	static CallPolarity fromBoolean(boolean positive) {
		return positive ? SimplePolarity.POSITIVE : SimplePolarity.NEGATIVE;
	}
}
