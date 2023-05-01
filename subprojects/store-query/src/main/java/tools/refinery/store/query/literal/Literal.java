/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;

public interface Literal {
	VariableBindingSite getVariableBindingSite();

	Literal substitute(Substitution substitution);

	default Literal reduce() {
		return this;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other);
}
