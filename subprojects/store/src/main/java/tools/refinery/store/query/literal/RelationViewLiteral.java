package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.List;
import java.util.Map;

public final class RelationViewLiteral extends CallLiteral<AnyRelationView>
		implements PolarLiteral<RelationViewLiteral> {
	public RelationViewLiteral(CallPolarity polarity, AnyRelationView target, List<Variable> arguments) {
		super(polarity, target, arguments);
	}

	@Override
	public RelationViewLiteral substitute(Map<Variable, Variable> substitution) {
		return new RelationViewLiteral(getPolarity(), getTarget(), substituteArguments(substitution));
	}

	@Override
	public RelationViewLiteral negate() {
		return new RelationViewLiteral(getPolarity().negate(), getTarget(), getArguments());
	}
}
