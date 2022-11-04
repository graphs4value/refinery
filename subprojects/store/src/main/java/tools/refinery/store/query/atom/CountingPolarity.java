package tools.refinery.store.query.atom;

public record CountingPolarity(ComparisonOperator operator, int threshold) implements CallPolarity {
	@Override
	public boolean isPositive() {
		return false;
	}

	@Override
	public boolean isTransitive() {
		return false;
	}
}
