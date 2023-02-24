package tools.refinery.store.partial.literal;

import tools.refinery.store.partial.representation.PartialRelation;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.PolarLiteral;
import tools.refinery.store.query.substitution.Substitution;

import java.util.List;

public final class ModalRelationLiteral extends ModalLiteral<PartialRelation>
		implements PolarLiteral<ModalRelationLiteral> {
	public ModalRelationLiteral(CallPolarity polarity, Modality modality, PartialRelation target,
						 List<Variable> arguments) {
		super(polarity, modality, target, arguments);
	}


	public ModalRelationLiteral(Modality modality, PartialRelationLiteral baseLiteral) {
		super(modality, baseLiteral);
	}

	@Override
	public Class<PartialRelation> getTargetType() {
		return PartialRelation.class;
	}

	@Override
	public ModalRelationLiteral substitute(Substitution substitution) {
		return new ModalRelationLiteral(getPolarity(), getModality(), getTarget(), substituteArguments(substitution));
	}

	@Override
	public ModalRelationLiteral negate() {
		return new ModalRelationLiteral(getPolarity().negate(), getModality(), getTarget(), getArguments());
	}
}
