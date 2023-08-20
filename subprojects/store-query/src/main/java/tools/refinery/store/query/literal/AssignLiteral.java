/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.equality.LiteralHashCodeHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.Variable;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public class AssignLiteral<T> extends AbstractLiteral {
	private final DataVariable<T> variable;
	private final Term<T> term;

	public AssignLiteral(DataVariable<T> variable, Term<T> term) {
		if (!term.getType().equals(variable.getType())) {
			throw new InvalidQueryException("Term %s must be of type %s, got %s instead".formatted(
					term, variable.getType().getName(), term.getType().getName()));
		}
		var inputVariables = term.getInputVariables();
		if (inputVariables.contains(variable)) {
			throw new InvalidQueryException("Result variable %s must not appear in the term %s".formatted(
					variable, term));
		}
		this.variable = variable;
		this.term = term;
	}

	public DataVariable<T> getVariable() {
		return variable;
	}

	public Term<T> getTerm() {
		return term;
	}

	@Override
	public Set<Variable> getOutputVariables() {
		return Set.of(variable);
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
		return new AssignLiteral<>(substitution.getTypeSafeSubstitute(variable), term.substitute(substitution));
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherAssignLiteral = (AssignLiteral<?>) other;
		return helper.variableEqual(variable, otherAssignLiteral.variable) &&
				term.equalsWithSubstitution(helper, otherAssignLiteral.term);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), helper.getVariableHashCode(variable),
				term.hashCodeWithSubstitution(helper));
	}

	@Override
	public String toString() {
		return "%s is (%s)".formatted(variable, term);
	}
}
