package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.List;

public final class RelationViewLiteral extends CallLiteral<AnyRelationView>
		implements PolarLiteral<RelationViewLiteral> {
	public RelationViewLiteral(CallPolarity polarity, AnyRelationView target, List<Variable> arguments) {
		super(polarity, target, arguments);
	}

	@Override
	public Class<AnyRelationView> getTargetType() {
		return AnyRelationView.class;
	}

	@Override
	protected String targetToString() {
		var target = getTarget();
		return "@RelationView(\"%s\") %s".formatted(target.getViewName(), target.getSymbol().name());
	}

	@Override
	public RelationViewLiteral substitute(Substitution substitution) {
		return new RelationViewLiteral(getPolarity(), getTarget(), substituteArguments(substitution));
	}

	@Override
	public RelationViewLiteral negate() {
		return new RelationViewLiteral(getPolarity().negate(), getTarget(), getArguments());
	}
}
