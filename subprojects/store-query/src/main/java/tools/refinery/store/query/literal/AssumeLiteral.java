/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.Variable;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record AssumeLiteral(Term<Boolean> term) implements Literal {
	public AssumeLiteral {
		if (!term.getType().equals(Boolean.class)) {
			throw new IllegalArgumentException("Term %s must be of type %s, got %s instead".formatted(
					term, Boolean.class.getName(), term.getType().getName()));
		}
	}

	@Override
	public Set<Variable> getOutputVariables() {
		return Set.of();
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Collections.unmodifiableSet(term.getInputVariables());
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Set.of();
	}


	@Override
	public Literal substitute(Substitution substitution) {
		return new AssumeLiteral(term.substitute(substitution));
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherAssumeLiteral = (AssumeLiteral) other;
		return term.equalsWithSubstitution(helper, otherAssumeLiteral.term);
	}

	@Override
	public Literal reduce() {
		if (term instanceof ConstantTerm<Boolean> constantTerm) {
			// Return {@link BooleanLiteral#FALSE} for {@code false} or {@code null} literals.
			return Boolean.TRUE.equals(constantTerm.getValue()) ? BooleanLiteral.TRUE :
					BooleanLiteral.FALSE;
		}
		return this;
	}

	@Override
	public String toString() {
		return "(%s)".formatted(term);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (AssumeLiteral) obj;
		return Objects.equals(this.term, that.term);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), term);
	}
}