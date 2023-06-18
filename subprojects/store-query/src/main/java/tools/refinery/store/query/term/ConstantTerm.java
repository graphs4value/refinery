/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.valuation.Valuation;

import java.util.Objects;
import java.util.Set;

public final class ConstantTerm<T> extends AbstractTerm<T> {
	private final T value;

	public ConstantTerm(Class<T> type, T value) {
		super(type);
		if (value != null && !type.isInstance(value)) {
			throw new IllegalArgumentException("Value %s is not an instance of %s".formatted(value, type.getName()));
		}
		this.value = value;
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
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherConstantTerm = (ConstantTerm<?>) other;
		return Objects.equals(value, otherConstantTerm.value);
	}

	@Override
	public Set<AnyDataVariable> getInputVariables() {
		return Set.of();
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ConstantTerm<?> that = (ConstantTerm<?>) o;
		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
}
