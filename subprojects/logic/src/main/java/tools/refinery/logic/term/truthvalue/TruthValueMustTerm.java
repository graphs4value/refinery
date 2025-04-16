package tools.refinery.logic.term.truthvalue;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;

public class TruthValueMustTerm extends UnaryTerm<Boolean, TruthValue> {
	protected TruthValueMustTerm(Term<TruthValue> body) {
		super(Boolean.class, TruthValue.class, body);
	}

	@Override
	protected Boolean doEvaluate(TruthValue bodyValue) {
		return bodyValue.must();
	}

	@Override
	protected Term<Boolean> constructWithBody(Term<TruthValue> newBody) {
		return new TruthValueMustTerm(newBody);
	}

	@Override
	public String toString() {
		return "(must %s)".formatted(getBody());
	}
}
