package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;

import java.util.Map;
import java.util.Set;

public class BooleanLiteral implements Literal {
	public static final BooleanLiteral TRUE = new BooleanLiteral(LiteralReduction.ALWAYS_TRUE);
	public static final BooleanLiteral FALSE = new BooleanLiteral(LiteralReduction.ALWAYS_FALSE);

	private final LiteralReduction reduction;

	private BooleanLiteral(LiteralReduction reduction) {
		this.reduction = reduction;
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		// No variables to collect.
	}

	@Override
	public Literal substitute(Map<Variable, Variable> substitution) {
		// No variables to substitute.
		return this;
	}

	@Override
	public LiteralReduction getReduction() {
		return reduction;
	}

	public static BooleanLiteral fromBoolean(boolean value) {
		return value ? TRUE : FALSE;
	}
}
