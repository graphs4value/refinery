package tools.refinery.store.query.term.real;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.UnaryTerm;

public class IntToRealTerm extends UnaryTerm<Double, Integer> {
	protected IntToRealTerm(Term<Integer> body) {
		super(body);
	}

	@Override
	public Class<Double> getType() {
		return Double.class;
	}

	@Override
	public Class<Integer> getBodyType() {
		return Integer.class;
	}

	@Override
	protected Term<Double> doSubstitute(Substitution substitution, Term<Integer> substitutedBody) {
		return new IntToRealTerm(substitutedBody);
	}

	@Override
	protected Double doEvaluate(Integer bodyValue) {
		return bodyValue.doubleValue();
	}

	@Override
	public String toString() {
		return "(%s) as real".formatted(getBody());
	}
}
