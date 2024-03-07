/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.substitution.Substitution;

import java.util.Set;

public sealed interface AnyTerm permits AnyDataVariable, Term {
	Class<?> getType();

	AnyTerm substitute(Substitution substitution);

	boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other);

	int hashCodeWithSubstitution(LiteralHashCodeHelper helper);

	Set<AnyDataVariable> getInputVariables();
}
