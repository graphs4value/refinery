package tools.refinery.store.query.term.real;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.ArithmeticUnaryOperator;
import tools.refinery.store.query.term.ArithmeticUnaryTerm;
import tools.refinery.store.query.term.Term;

public class RealArithmeticUnaryTerm extends ArithmeticUnaryTerm<Double> {
	public RealArithmeticUnaryTerm(ArithmeticUnaryOperator operation, Term<Double> body) {
		super(operation, body);
	}

	@Override
	public Class<Double> getType() {
		return Double.class;
	}

	@Override
	protected Term<Double> doSubstitute(Substitution substitution, Term<Double> substitutedBody) {
		return new RealArithmeticUnaryTerm(getOperator(), substitutedBody);
	}

	@Override
	protected Double doEvaluate(Double bodyValue) {
		return switch(getOperator()) {
			case PLUS -> bodyValue;
			case MINUS -> -bodyValue;
		};
	}
}
