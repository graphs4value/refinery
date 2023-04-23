/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;

import java.util.Objects;

public abstract class AbstractTerm<T> implements Term<T> {
	private final Class<T> type;

	protected AbstractTerm(Class<T> type) {
		this.type = type;
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		return getClass().equals(other.getClass()) && type.equals(other.getType());
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractTerm<?> that = (AbstractTerm<?>) o;
		return type.equals(that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), type);
	}
}
