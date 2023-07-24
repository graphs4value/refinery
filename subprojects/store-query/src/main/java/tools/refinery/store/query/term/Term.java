/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import tools.refinery.store.query.literal.AssignLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.valuation.Valuation;

public non-sealed interface Term<T> extends AnyTerm, AssignedValue<T> {
	@Override
	Class<T> getType();

	T evaluate(Valuation valuation);

	@Override
	Term<T> substitute(Substitution substitution);

	@Override
	default Literal toLiteral(DataVariable<T> targetVariable) {
		return new AssignLiteral<>(targetVariable, this);
	}
}
