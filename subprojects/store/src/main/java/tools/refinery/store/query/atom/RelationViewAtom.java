package tools.refinery.store.query.atom;

import tools.refinery.store.query.Variable;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.List;

public final class RelationViewAtom extends CallAtom<AnyRelationView> {
	public RelationViewAtom(CallPolarity polarity, AnyRelationView target, List<Variable> substitution) {
		super(polarity, target, substitution);
	}

	public RelationViewAtom(CallPolarity polarity, AnyRelationView target, Variable... substitution) {
		super(polarity, target, substitution);
	}

	public RelationViewAtom(boolean positive, AnyRelationView target, List<Variable> substitution) {
		super(positive, target, substitution);
	}

	public RelationViewAtom(boolean positive, AnyRelationView target, Variable... substitution) {
		super(positive, target, substitution);
	}

	public RelationViewAtom(AnyRelationView target, List<Variable> substitution) {
		super(target, substitution);
	}

	public RelationViewAtom(AnyRelationView target, Variable... substitution) {
		super(target, substitution);
	}
}
