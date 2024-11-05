/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.util.CallSite;
import tools.refinery.logic.valuation.Valuation;

import java.util.List;
import java.util.Set;

// {@link Object#equals(Object)} is implemented by {@link AbstractTerm}.
@SuppressWarnings("squid:S2160")
public abstract class AbstractCallTerm<T> extends AbstractTerm<T> {
	private final CallSite callSite;

	protected AbstractCallTerm(Class<T> type, Constraint target, List<Variable> arguments) {
		super(type);
		callSite = new CallSite(target, arguments);
	}

	public Constraint getTarget() {
		return callSite.getTarget();
	}

	public List<Variable> getArguments() {
		return callSite.getArguments();
	}

	protected Set<Variable> getArgumentsOfDirection(ParameterDirection direction) {
		return callSite.getArgumentsOfDirection(direction);
	}

	@Override
	public T evaluate(Valuation valuation) {
		throw new IllegalStateException("Call term %s cannot be evaluated directly.".formatted(this));
	}

	@Override
	public Term<T> rewriteSubTerms(TermRewriter termRewriter) {
		// No sub-terms to rewrite.
		return this;
	}

	@Override
	public Term<T> substitute(Substitution substitution) {
		var substitutedArguments = callSite.getSubstitutedArguments(substitution);
		return doSubstitute(substitution, substitutedArguments);
	}

	protected abstract Term<T> doSubstitute(Substitution substitution, List<Variable> substitutedArguments);

	@Override
	public Set<Variable> getVariables() {
		return Set.copyOf(getArguments());
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		return callSite.getInputVariablesForNonEnumerableCall(positiveVariablesInClause);
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return callSite.getPrivateVariablesForNonEnumerableCall(positiveVariablesInClause);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherCallTerm = (AbstractCallTerm<?>) other;
		return callSite.equalsWithSubstitution(helper, otherCallTerm.callSite);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return super.hashCodeWithSubstitution(helper) * 31 + callSite.hashCodeWithSubstitution(helper);
	}
}
