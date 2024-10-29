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

/**
 * In mathematical logic, a literal is an atomic formula or its negation. A literal is a formula that is either true
 * or false, but not both. It is a basic building block of logic.
 */

/**
 * A clause is a propositional (well-formed) formula formed from a finite collection of literals and logical connectives
 */
public interface Literal {
	Set<Variable> getOutputVariables();

	Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause);

	Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause);

	Literal substitute(Substitution substitution);

	//The literal itself is not reducable!
	default Literal reduce() {
		return this;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other);

	int hashCodeWithSubstitution(LiteralHashCodeHelper helper);
}
