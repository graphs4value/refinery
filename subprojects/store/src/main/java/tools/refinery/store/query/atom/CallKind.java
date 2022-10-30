package tools.refinery.store.query.atom;

public sealed interface CallKind permits BasicCallKind, CountCallKind {
	boolean isPositive();

	boolean isTransitive();

	static CallKind fromBoolean(boolean positive) {
		return positive ? BasicCallKind.POSITIVE : BasicCallKind.NEGATIVE;
	}
}
