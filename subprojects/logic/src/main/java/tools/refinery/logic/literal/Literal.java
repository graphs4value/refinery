/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.literal;

import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.Variable;

import java.util.Set;

public interface Literal {
	Set<Variable> getOutputVariables();

	Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause);

	Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause);

	Literal substitute(Substitution substitution);

	default Literal reduce() {
		return this;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other);

	int hashCodeWithSubstitution(LiteralHashCodeHelper helper);
}
