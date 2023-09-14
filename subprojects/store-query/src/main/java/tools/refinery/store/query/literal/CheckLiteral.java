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
import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.bool.BoolNotTerm;
import tools.refinery.store.query.term.bool.BoolTerms;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public class CheckLiteral extends AbstractLiteral implements CanNegate<CheckLiteral> {
	private final Term<Boolean> term;

	public CheckLiteral(Term<Boolean> term) {
		if (!term.getType().equals(Boolean.class)) {
			throw new InvalidQueryException("Term %s must be of type %s, got %s instead".formatted(
					term, Boolean.class.getName(), term.getType().getName()));
		}
		this.term = term;
	}

	public Term<Boolean> getTerm() {
		return term;
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
		return new CheckLiteral(term.substitute(substitution));
	}

	@Override
	public CheckLiteral negate() {
		if (term instanceof BoolNotTerm notTerm) {
			return new CheckLiteral(notTerm.getBody());
		}
		return new CheckLiteral(BoolTerms.not(term));
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherAssumeLiteral = (CheckLiteral) other;
		return term.equalsWithSubstitution(helper, otherAssumeLiteral.term);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), term.hashCodeWithSubstitution(helper));
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
}
