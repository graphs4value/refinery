package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Conjunction;

public record WrappedConjunction(Conjunction conjunction) {
	public Conjunction get() {
		return conjunction;
	}
	
	public WrappedLiteral lit(int i) {
		return new WrappedLiteral(conjunction.getLiterals().get(i));
	}
}
