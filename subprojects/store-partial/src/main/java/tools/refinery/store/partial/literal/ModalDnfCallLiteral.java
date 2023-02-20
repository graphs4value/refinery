package tools.refinery.store.partial.literal;

import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.DnfCallLiteral;
import tools.refinery.store.query.literal.LiteralReduction;
import tools.refinery.store.query.literal.PolarLiteral;

import java.util.List;
import java.util.Map;

public class ModalDnfCallLiteral extends ModalLiteral<Dnf> implements PolarLiteral<ModalDnfCallLiteral> {
	public ModalDnfCallLiteral(CallPolarity polarity, Modality modality, Dnf target, List<Variable> arguments) {
		super(polarity, modality, target, arguments);
	}

	public ModalDnfCallLiteral(Modality modality, DnfCallLiteral baseLiteral) {
		super(modality.commute(baseLiteral.getPolarity()), baseLiteral);
	}

	@Override
	public ModalDnfCallLiteral substitute(Map<Variable, Variable> substitution) {
		return new ModalDnfCallLiteral(getPolarity(), getModality(), getTarget(), substituteArguments(substitution));
	}

	@Override
	public ModalDnfCallLiteral negate() {
		return new ModalDnfCallLiteral(getPolarity().negate(), getModality(), getTarget(), getArguments());
	}

	@Override
	public LiteralReduction getReduction() {
		var dnfReduction = getTarget().getReduction();
		return getPolarity().isPositive() ? dnfReduction : dnfReduction.negate();
	}
}
