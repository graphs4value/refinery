/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.substitution.Substitution;

import java.util.Set;

public sealed interface AnyTerm permits AnyDataVariable, Term {
	Class<?> getType();

	AnyTerm rewriteSubTerms(TermRewriter termRewriter);

	AnyTerm substitute(Substitution substitution);

	boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other);

	int hashCodeWithSubstitution(LiteralHashCodeHelper helper);

	Set<Variable> getVariables();

	default Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		return getVariables();
	}

	default Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Set.of();
	}

	default <T> Term<T> asType(Class<T> type) {
		if (!type.equals(getType())) {
			throw new IllegalArgumentException("Tried to cast to %s but the term is of type %s."
					.formatted(type.getName(), getType().getName()));
		}
		// We have just checked the type explicitly above.
		@SuppressWarnings("unchecked")
		var term = (Term<T>) this;
		return term;
	}
}
