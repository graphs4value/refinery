package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.UnaryTerm;

public class RealToIntTerm extends UnaryTerm<Integer, Double> {
	protected RealToIntTerm(Term<Double> body) {
		super(body);
	}

	@Override
	public Class<Integer> getType() {
		return Integer.class;
	}

	@Override
	public Class<Double> getBodyType() {
		return Double.class;
	}

	@Override
	protected Integer doEvaluate(Double bodyValue) {
		return bodyValue.intValue();
	}

	@Override
	protected Term<Integer> doSubstitute(Substitution substitution, Term<Double> substitutedBody) {
		return new RealToIntTerm(substitutedBody);
	}

	@Override
	public String toString() {
		return "(%s) as int".formatted(getBody());
	}
}
