/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.pquery;

import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.AnyTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.valuation.Valuation;

import java.util.Set;

/**
 * Term wrapper that prevents {@link Term#equalsWithSubstitution(LiteralEqualityHelper, AnyTerm)} from being
 * able to deduce the equality of two terms.
 * <p>
 * This can control the shape of {@link tools.refinery.logic.dnf.Dnf} precisely when we don't want the optimizer
 * to reduce multiple equivalent assignments into a single one.
 * </p>
 *
 * @param <T> The type of the term.
 */
public record OptimizationBarrier<T>(Term<T> body) implements Term<T> {
	@Override
	public Class<T> getType() {
		return body.getType();
	}

	@Override
	public T evaluate(Valuation valuation) {
		return body.evaluate(valuation);
	}

	@Override
	public Term<T> rewriteSubTerms(TermRewriter termRewriter) {
		var result = body.rewriteSubTerms(termRewriter);
		return body.equals(result) ? this : new OptimizationBarrier<>(result);
	}

	@Override
	public Term<T> substitute(Substitution substitution) {
		var result = body.substitute(substitution);
		return body.equals(result) ? this : new OptimizationBarrier<>(result);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		return equals(other);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return hashCode();
	}

	@Override
	public Set<Variable> getVariables() {
		return body.getVariables();
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		return body.getInputVariables(positiveVariablesInClause);
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return body.getPrivateVariables(positiveVariablesInClause);
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public @NotNull String toString() {
		return "@OptimizationBarrier(%s)".formatted(body);
	}
}
