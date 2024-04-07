/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf;

import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.Variable;

import java.util.List;
import java.util.Set;

public record DnfClause(Set<Variable> positiveVariables, List<Literal> literals) {
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, DnfClause other) {
		int size = literals.size();
		if (size != other.literals.size()) {
			return false;
		}
		for (int i = 0; i < size; i++) {
			if (!literals.get(i).equalsWithSubstitution(helper, other.literals.get(i))) {
				return false;
			}
		}
		return true;
	}

	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		int result = 0;
		for (var literal : literals) {
			result = result * 31 + literal.hashCodeWithSubstitution(helper);
		}
		return result;
	}
}
