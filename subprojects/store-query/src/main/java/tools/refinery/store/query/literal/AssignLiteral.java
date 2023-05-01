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

import java.util.Objects;

public final class AssignLiteral<T> implements Literal {
	private final DataVariable<T> variable;
	private final Term<T> term;
	private final VariableBindingSite variableBindingSite;

	public AssignLiteral(DataVariable<T> variable, Term<T> term) {
		if (!term.getType().equals(variable.getType())) {
			throw new IllegalArgumentException("Term %s must be of type %s, got %s instead".formatted(
					term, variable.getType().getName(), term.getType().getName()));
		}
		this.variable = variable;
		this.term = term;
		var inputVariables = term.getInputVariables();
		if (inputVariables.contains(variable)) {
			throw new IllegalArgumentException("Result variable %s must not appear in the term %s".formatted(
					variable, term));
		}
		variableBindingSite = VariableBindingSite.builder()
				.variable(variable, VariableDirection.OUT)
				.variables(inputVariables, VariableDirection.IN)
				.build();
	}

	public DataVariable<T> getTargetVariable() {
		return variable;
	}

	public Term<T> getTerm() {
		return term;
	}

	@Override
	public VariableBindingSite getVariableBindingSite() {
		return variableBindingSite;
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (AssignLiteral<?>) obj;
		return Objects.equals(this.variable, that.variable) &&
				Objects.equals(this.term, that.term);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), variable, term);
	}
}
