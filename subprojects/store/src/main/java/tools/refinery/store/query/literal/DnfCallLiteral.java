package tools.refinery.store.query.literal;

import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.Variable;

import java.util.List;

public final class DnfCallLiteral extends CallLiteral<Dnf> implements PolarLiteral<DnfCallLiteral> {
	public DnfCallLiteral(CallPolarity polarity, Dnf target, List<Variable> substitution) {
		super(polarity, target, substitution);
	}

	@Override
	public DnfCallLiteral negate() {
		return new DnfCallLiteral(getPolarity().negate(), getTarget(), getSubstitution());
	}
}
