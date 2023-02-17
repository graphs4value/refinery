package tools.refinery.store.query.literal;

import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.Variable;

import java.util.List;
import java.util.Map;

public final class DnfCallLiteral extends CallLiteral<Dnf> implements PolarLiteral<DnfCallLiteral> {
	public DnfCallLiteral(CallPolarity polarity, Dnf target, List<Variable> arguments) {
		super(polarity, target, arguments);
	}

	@Override
	public DnfCallLiteral substitute(Map<Variable, Variable> substitution) {
		return new DnfCallLiteral(getPolarity(), getTarget(), substituteArguments(substitution));
	}

	@Override
	public DnfCallLiteral negate() {
		return new DnfCallLiteral(getPolarity().negate(), getTarget(), getArguments());
	}
}
