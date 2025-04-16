package tools.refinery.logic.term.truthvalue;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;

public class TruthValueNotTerm extends UnaryTerm<TruthValue, TruthValue> {
	protected TruthValueNotTerm(Term<TruthValue> body) {
		super(TruthValue.class, TruthValue.class, body);
	}

	@Override
	protected TruthValue doEvaluate(TruthValue bodyValue) {
		return bodyValue.not();
	}

	@Override
	protected Term<TruthValue> constructWithBody(Term<TruthValue> newBody) {
		return new TruthValueNotTerm(newBody);
	}

	@Override
	public String toString() {
		return "(!%s)".formatted(getBody());
	}
}
