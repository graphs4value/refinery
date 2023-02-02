package tools.refinery.store.representation;

import java.util.Objects;

public final class Symbol<T> implements AnySymbol {
	private final String name;
	private final int arity;
	private final Class<T> valueType;
	private final T defaultValue;

	public Symbol(String name, int arity, Class<T> valueType, T defaultValue) {
		this.name = name;
		this.arity = arity;
		this.valueType = valueType;
		this.defaultValue = defaultValue;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public int arity() {
		return arity;
	}

	@Override
	public Class<T> valueType() {
		return valueType;
	}

	public T defaultValue() {
		return defaultValue;
	}

	public boolean isDefaultValue(T value) {
		return Objects.equals(defaultValue, value);
	}

	@Override
	public String toString() {
		return "%s/%d".formatted(name, arity);
	}
}
