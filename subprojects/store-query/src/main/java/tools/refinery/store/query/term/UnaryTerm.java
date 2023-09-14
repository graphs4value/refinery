/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.equality.LiteralHashCodeHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.valuation.Valuation;

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
	public Term<R> substitute(Substitution substitution) {
		return doSubstitute(substitution, body.substitute(substitution));
	}

	protected abstract Term<R> doSubstitute(Substitution substitution, Term<T> substitutedBody);

	@Override
	public Set<AnyDataVariable> getInputVariables() {
		return body.getInputVariables();
	}
}
