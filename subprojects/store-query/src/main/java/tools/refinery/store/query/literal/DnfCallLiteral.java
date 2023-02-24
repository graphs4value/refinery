package tools.refinery.store.query.literal;

import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;

import java.util.List;

public final class DnfCallLiteral extends CallLiteral<Dnf> implements PolarLiteral<DnfCallLiteral> {
	public DnfCallLiteral(CallPolarity polarity, Dnf target, List<Variable> arguments) {
		super(polarity, target, arguments);
	}

	@Override
	public Class<Dnf> getTargetType() {
		return Dnf.class;
	}

	@Override
	public DnfCallLiteral substitute(Substitution substitution) {
		return new DnfCallLiteral(getPolarity(), getTarget(), substituteArguments(substitution));
	}

	@Override
	public DnfCallLiteral negate() {
		return new DnfCallLiteral(getPolarity().negate(), getTarget(), getArguments());
	}

	@Override
	public LiteralReduction getReduction() {
		var dnfReduction = getTarget().getReduction();
		return getPolarity().isPositive() ? dnfReduction : dnfReduction.negate();
	}

	@Override
	protected boolean targetEquals(LiteralEqualityHelper helper, Dnf otherTarget) {
		return helper.dnfEqual(getTarget(), otherTarget);
	}
}
