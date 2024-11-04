/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.valuation.Valuation;

import java.util.Objects;
import java.util.Set;

// {@link Object#equals(Object)} is implemented by {@link AbstractTerm}.
@SuppressWarnings("squid:S2160")
public abstract class UnaryTerm<R, T> extends AbstractTerm<R> {
	private final Class<T> bodyType;
	private final Term<T> body;

	protected UnaryTerm(Class<R> type, Class<T> bodyType, Term<T> body) {
		super(type);
		if (!body.getType().equals(bodyType)) {
			throw new InvalidQueryException("Expected body %s to be of type %s, got %s instead".formatted(body,
					bodyType.getName(), body.getType().getName()));
		}
		this.bodyType = bodyType;
		this.body = body;
	}

	public Class<T> getBodyType() {
		return bodyType;
	}

	public Term<T> getBody() {
		return body;
	}

	@Override
	public R evaluate(Valuation valuation) {
		var bodyValue = body.evaluate(valuation);
		return bodyValue == null ? null : doEvaluate(bodyValue);
	}

	protected abstract R doEvaluate(T bodyValue);

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherUnaryTerm = (UnaryTerm<?, ?>) other;
		return bodyType.equals(otherUnaryTerm.bodyType) && body.equalsWithSubstitution(helper, otherUnaryTerm.body);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), bodyType, body.hashCodeWithSubstitution(helper));
	}

	@Override
	public Term<R> rewriteSubTerms(TermRewriter termRewriter) {
		return withBody(body.rewriteSubTerms(termRewriter));
	}

	@Override
	public Term<R> substitute(Substitution substitution) {
		return withBody(body.substitute(substitution));
	}

	public abstract Term<R> withBody(Term<T> newBody);

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
}
