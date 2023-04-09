/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.Variable;

import java.util.Set;

public record AssignLiteral<T>(DataVariable<T> variable, Term<T> term) implements Literal {
	public AssignLiteral {
		if (!term.getType().equals(variable.getType())) {
			throw new IllegalArgumentException("Term %s must be of type %s, got %s instead".formatted(
					term, variable.getType().getName(), term.getType().getName()));
		}
	}

	@Override
	public Set<Variable> getBoundVariables() {
		return Set.of(variable);
	}

	@Override
	public Literal substitute(Substitution substitution) {
		return new AssignLiteral<>(substitution.getTypeSafeSubstitute(variable), term.substitute(substitution));
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherLetLiteral = (AssignLiteral<?>) other;
		return helper.variableEqual(variable, otherLetLiteral.variable) && term.equalsWithSubstitution(helper,
				otherLetLiteral.term);
	}


	@Override
	public String toString() {
		return "%s is (%s)".formatted(variable, term);
	}
}
