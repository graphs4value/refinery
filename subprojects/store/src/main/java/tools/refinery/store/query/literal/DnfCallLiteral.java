package tools.refinery.store.query.literal;

import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.Variable;

import java.util.List;

public final class DnfCallLiteral extends CallLiteral<Dnf> {
	public DnfCallLiteral(CallPolarity polarity, Dnf target, List<Variable> substitution) {
		super(polarity, target, substitution);
	}

	public DnfCallLiteral(CallPolarity polarity, Dnf target, Variable... substitution) {
		this(polarity, target, List.of(substitution));
	}

	public DnfCallLiteral(boolean positive, Dnf target, List<Variable> substitution) {
		this(CallPolarity.fromBoolean(positive), target, substitution);
	}

	public DnfCallLiteral(boolean positive, Dnf target, Variable... substitution) {
		this(positive, target, List.of(substitution));
	}

	public DnfCallLiteral(Dnf target, List<Variable> substitution) {
		this(CallPolarity.POSITIVE, target, substitution);
	}

	public DnfCallLiteral(Dnf target, Variable... substitution) {
		this(target, List.of(substitution));
	}
}
