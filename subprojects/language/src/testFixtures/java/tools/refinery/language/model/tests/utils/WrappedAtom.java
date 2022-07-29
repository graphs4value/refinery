package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Atom;

public record WrappedAtom(Atom atom) {
	public Atom get() {
		return atom;
	}
	
	public WrappedArgument arg(int i) {
		return new WrappedArgument(atom.getArguments().get(i));
	}
}
