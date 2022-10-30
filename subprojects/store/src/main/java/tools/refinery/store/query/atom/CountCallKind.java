package tools.refinery.store.query.atom;

public record CountCallKind(ComparisonOperator operator, int threshold) implements CallKind {
	@Override
	public boolean isPositive() {
		return false;
	}

	@Override
	public boolean isTransitive() {
		return false;
	}
}
