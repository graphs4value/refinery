package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.Expr;
import tools.refinery.language.model.problem.NegationExpr;

public record WrappedLiteral(Expr expr) {
	public Expr get() {
		return expr;
	}

	public WrappedAtom atom() {
		return new WrappedAtom((Atom) expr);
	}

	public WrappedAtom negated() {
		return new WrappedAtom((Atom) ((NegationExpr) expr).getBody());
	}

	public WrappedArgument arg(int i) {
		return atom().arg(i);
	}
}
