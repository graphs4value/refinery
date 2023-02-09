package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.List;

public final class RelationViewLiteral extends CallLiteral<AnyRelationView> {
	public RelationViewLiteral(CallPolarity polarity, AnyRelationView target, List<Variable> substitution) {
		super(polarity, target, substitution);
	}

	public RelationViewLiteral(CallPolarity polarity, AnyRelationView target, Variable... substitution) {
		this(polarity, target, List.of(substitution));
	}

	public RelationViewLiteral(boolean positive, AnyRelationView target, List<Variable> substitution) {
		this(CallPolarity.fromBoolean(positive), target, substitution);
	}

	public RelationViewLiteral(boolean positive, AnyRelationView target, Variable... substitution) {
		this(positive, target, List.of(substitution));
	}

	public RelationViewLiteral(AnyRelationView target, List<Variable> substitution) {
		this(CallPolarity.POSITIVE, target, substitution);
	}

	public RelationViewLiteral(AnyRelationView target, Variable... substitution) {
		this(target, List.of(substitution));
	}
}
