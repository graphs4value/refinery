package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.Literal;
import tools.refinery.language.model.problem.NegativeLiteral;

public record WrappedLiteral(Literal literal) {
	public Literal get() {
		return literal;
	}
	
	public WrappedAtom atom() {
		return new WrappedAtom((Atom) literal);
	}

	public WrappedAtom negated() {
		return new WrappedAtom(((NegativeLiteral) literal).getAtom());
	}

	public WrappedArgument arg(int i) {
		return atom().arg(i);
	}
}
