package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.valuation.Valuation;

import java.util.Set;

public record ConstantTerm<T>(Class<T> type, T value) implements Term<T> {
	public ConstantTerm {
		if (value == null) {
			throw new IllegalArgumentException("value should not be null");
		}
		if (!type.isInstance(value)) {
			throw new IllegalArgumentException("value %s is not an instance of %s".formatted(value, type.getName()));
		}
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	public T getValue() {
		return value;
	}

	@Override
	public T evaluate(Valuation valuation) {
		return getValue();
	}

	@Override
	public Term<T> substitute(Substitution substitution) {
		return this;
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		return equals(other);
	}

	@Override
	public Set<AnyDataVariable> getInputVariables() {
		return Set.of();
	}

	@Override
	public String toString() {
		return getValue().toString();
	}
}
