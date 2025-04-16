package tools.refinery.logic.term.truthvalue;

import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;

public class TruthValueTerms {
	private TruthValueTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static Term<TruthValue> constant(TruthValue value) {
		return new ConstantTerm<>(TruthValue.class, value);
	}

	public static Term<TruthValue> not(Term<TruthValue> body) {
		return new TruthValueNotTerm(body);
	}

	public static Term<Boolean> may(Term<TruthValue> body) {
		return new TruthValueMayTerm(body);
	}

	public static Term<Boolean> must(Term<TruthValue> body) {
		return new TruthValueMustTerm(body);
	}
}
