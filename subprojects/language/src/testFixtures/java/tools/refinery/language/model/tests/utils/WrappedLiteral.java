package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.Literal;
import tools.refinery.language.model.problem.NegativeLiteral;
import tools.refinery.language.model.problem.ValueLiteral;

public record WrappedLiteral(Literal literal) {
	public Literal get() {
		return literal;
	}

	public WrappedAtom valueAtom() {
		return new WrappedAtom(((ValueLiteral) literal).getAtom());
	}

	public WrappedAtom negated() {
		return new WrappedAtom(((NegativeLiteral) literal).getAtom());
	}

	public WrappedArgument arg(int i) {
		return new WrappedAtom((Atom) literal).arg(i);
	}
}
