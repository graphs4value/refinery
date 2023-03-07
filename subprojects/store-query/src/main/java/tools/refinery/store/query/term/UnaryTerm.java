package tools.refinery.store.query.term;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.valuation.Valuation;

import java.util.Objects;
import java.util.Set;

public abstract class UnaryTerm<R, T> implements Term<R> {
	private final Term<T> body;

	protected UnaryTerm(Term<T> body) {
		if (!body.getType().equals(getBodyType())) {
			throw new IllegalArgumentException("Expected body %s to be of type %s, got %s instead".formatted(body,
					getBodyType().getName(), body.getType().getName()));
		}
		this.body = body;
	}

	public abstract Class<T> getBodyType();

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
		if (getClass() != other.getClass()) {
			return false;
		}
		var otherUnaryTerm = (UnaryTerm<?, ?>) other;
		return body.equalsWithSubstitution(helper, otherUnaryTerm.body);
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
		UnaryTerm<?, ?> unaryTerm = (UnaryTerm<?, ?>) o;
		return body.equals(unaryTerm.body);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), body);
	}
}
