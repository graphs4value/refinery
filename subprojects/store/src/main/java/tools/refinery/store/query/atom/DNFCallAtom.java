package tools.refinery.store.query.atom;

import tools.refinery.store.query.DNF;
import tools.refinery.store.query.Variable;

import java.util.List;

public class DNFCallAtom extends CallAtom<DNF> {
	public DNFCallAtom(CallPolarity polarity, DNF target, List<Variable> substitution) {
		super(polarity, target, substitution);
	}

	public DNFCallAtom(CallPolarity polarity, DNF target, Variable... substitution) {
		super(polarity, target, substitution);
	}

	public DNFCallAtom(boolean positive, DNF target, List<Variable> substitution) {
		super(positive, target, substitution);
	}

	public DNFCallAtom(boolean positive, DNF target, Variable... substitution) {
		super(positive, target, substitution);
	}

	public DNFCallAtom(DNF target, List<Variable> substitution) {
		super(target, substitution);
	}

	public DNFCallAtom(DNF target, Variable... substitution) {
		super(target, substitution);
	}
}
