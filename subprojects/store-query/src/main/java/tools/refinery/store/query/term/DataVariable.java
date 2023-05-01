/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.valuation.Valuation;

import java.util.Objects;

public final class DataVariable<T> extends AnyDataVariable implements Term<T> {
	private final Class<T> type;

	DataVariable(String name, Class<T> type) {
		super(name);
		this.type = type;
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public DataVariable<T> renew(@Nullable String name) {
		return new DataVariable<>(name, type);
	}

	@Override
	public DataVariable<T> renew() {
		return renew(getExplicitName());
	}

	@Override
	public <U> DataVariable<U> asDataVariable(Class<U> newType) {
		if (!getType().equals(newType)) {
			throw new IllegalStateException("%s is not of type %s but of type %s".formatted(this, newType.getName(),
					getType().getName()));
		}
		@SuppressWarnings("unchecked")
		var result = (DataVariable<U>) this;
		return result;
	}

	@Override
	public T evaluate(Valuation valuation) {
		return valuation.getValue(this);
	}

	@Override
	public Term<T> substitute(Substitution substitution) {
		return substitution.getTypeSafeSubstitute(this);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		return other instanceof DataVariable<?> dataVariable && helper.variableEqual(this, dataVariable);
	}

	public Literal assign(AssignedValue<T> value) {
		return value.toLiteral(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		DataVariable<?> that = (DataVariable<?>) o;
		return type.equals(that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), type);
	}
}
