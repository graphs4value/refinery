package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.List;

public final class RelationViewLiteral extends CallLiteral<AnyRelationView>
		implements PolarLiteral<RelationViewLiteral> {
	public RelationViewLiteral(CallPolarity polarity, AnyRelationView target, List<Variable> substitution) {
		super(polarity, target, substitution);
	}

	@Override
	public RelationViewLiteral negate() {
		return new RelationViewLiteral(getPolarity().negate(), getTarget(), getSubstitution());
	}
}
