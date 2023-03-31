package tools.refinery.store.query.term;

import org.jetbrains.annotations.Nullable;

public record DataSort<T>(Class<T> type) implements Sort {
	public static final DataSort<Integer> INT = new DataSort<>(Integer.class);

	public static final DataSort<Boolean> BOOL = new DataSort<>(Boolean.class);

	@Override
	public boolean isInstance(Variable variable) {
		return variable instanceof DataVariable<?> dataVariable && type.equals(dataVariable.getType());
	}

	@Override
	public DataVariable<T> newInstance(@Nullable String name) {
		return Variable.of(name, type);
	}

	@Override
	public DataVariable<T> newInstance() {
		return newInstance(null);
	}

	@Override
	public String toString() {
		return type.getName();
	}
}
