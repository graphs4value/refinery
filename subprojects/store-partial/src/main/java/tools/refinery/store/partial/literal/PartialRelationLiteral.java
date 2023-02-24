package tools.refinery.store.partial.literal;

import tools.refinery.store.partial.representation.PartialRelation;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.literal.CallLiteral;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.PolarLiteral;
import tools.refinery.store.query.substitution.Substitution;

import java.util.List;

public final class PartialRelationLiteral extends CallLiteral<PartialRelation>
		implements PolarLiteral<PartialRelationLiteral> {
	public PartialRelationLiteral(CallPolarity polarity, PartialRelation target, List<Variable> substitution) {
		super(polarity, target, substitution);
	}

	@Override
	public Class<PartialRelation> getTargetType() {
		return PartialRelation.class;
	}

	@Override
	public PartialRelationLiteral substitute(Substitution substitution) {
		return new PartialRelationLiteral(getPolarity(), getTarget(), substituteArguments(substitution));
	}

	@Override
	public PartialRelationLiteral negate() {
		return new PartialRelationLiteral(getPolarity().negate(), getTarget(), getArguments());
	}
}
