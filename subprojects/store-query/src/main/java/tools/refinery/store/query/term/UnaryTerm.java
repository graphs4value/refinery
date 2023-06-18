/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.valuation.Valuation;

import java.util.Objects;
import java.util.Set;

public abstract class UnaryTerm<R, T> extends AbstractTerm<R> {
	private final Class<T> bodyType;
	private final Term<T> body;

	protected UnaryTerm(Class<R> type, Class<T> bodyType, Term<T> body) {
		super(type);
		if (!body.getType().equals(bodyType)) {
			throw new IllegalArgumentException("Expected body %s to be of type %s, got %s instead".formatted(body,
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
	public Term<R> substitute(Substitution substitution) {
		return doSubstitute(substitution, body.substitute(substitution));
	}

	protected abstract Term<R> doSubstitute(Substitution substitution, Term<T> substitutedBody);

	@Override
	public Set<AnyDataVariable> getInputVariables() {
		return body.getInputVariables();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		UnaryTerm<?, ?> unaryTerm = (UnaryTerm<?, ?>) o;
		return Objects.equals(bodyType, unaryTerm.bodyType) && Objects.equals(body, unaryTerm.body);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), bodyType, body);
	}
}
