/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.literal.*;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueNotTerm;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;

import java.util.Objects;
import java.util.Set;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public class PartialCheckLiteral extends AbstractLiteral implements CanNegate<PartialCheckLiteral>, TermLiteral<TruthValue> {
	private final Term<TruthValue> term;

	public PartialCheckLiteral(Term<TruthValue> term) {
		if (!term.getType().equals(TruthValue.class)) {
			throw new InvalidQueryException("Term %s must be of type %s, got %s instead".formatted(
					term, TruthValue.class.getName(), term.getType().getName()));
		}
		this.term = term.reduce();
	}

	@Override
	public Term<TruthValue> getTerm() {
		return term;
	}

	@Override
	public PartialCheckLiteral withTerm(Term<TruthValue> term) {
		if (this.term == term) {
			return this;
		}
		return new PartialCheckLiteral(term);
	}

	@Override
	public Set<Variable> getOutputVariables() {
		return Set.of();
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		return term.getInputVariables(positiveVariablesInClause);
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return term.getPrivateVariables(positiveVariablesInClause);
	}

	@Override
	public Literal substitute(Substitution substitution) {
		return new PartialCheckLiteral(term.substitute(substitution));
	}

	@Override
	public PartialCheckLiteral negate() {
		if (term instanceof TruthValueNotTerm notTerm) {
			return new PartialCheckLiteral(notTerm.getBody());
		}
		return new PartialCheckLiteral(TruthValueTerms.not(term));
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherAssumeLiteral = (PartialCheckLiteral) other;
		return term.equalsWithSubstitution(helper, otherAssumeLiteral.term);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), term.hashCodeWithSubstitution(helper));
	}

	@Override
	public Literal reduce() {
		if (term instanceof ConstantTerm<TruthValue> constantTerm) {
			// Return {@link BooleanLiteral#FALSE} for {@code FALSE} or {@code null} literals.
			return switch (constantTerm.getValue()) {
				case TRUE -> BooleanLiteral.TRUE;
				case FALSE -> BooleanLiteral.FALSE;
				case null -> BooleanLiteral.FALSE;
				default -> this;
			};
		}
		return this;
	}

	@Override
	public String toString() {
		return "(%s)".formatted(term);
	}
}
