/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.equality.LiteralHashCodeHelper;

public abstract class AbstractLiteral implements Literal {
	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		return other != null && getClass() == other.getClass();
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return getClass().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractLiteral that = (AbstractLiteral) o;
		return equalsWithSubstitution(LiteralEqualityHelper.DEFAULT, that);
	}

	@Override
	public int hashCode() {
		return hashCodeWithSubstitution(LiteralHashCodeHelper.DEFAULT);
	}
}
