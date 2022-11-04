package tools.refinery.store.query.atom;

import tools.refinery.store.model.RelationLike;
import tools.refinery.store.query.Variable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CallAtom<T extends RelationLike> extends AbstractSubstitutionAtom<T> {
	private final CallPolarity polarity;

	public CallAtom(CallPolarity polarity, T target, List<Variable> substitution) {
		super(target, substitution);
		if (polarity.isTransitive() && target.getArity() != 2) {
			throw new IllegalArgumentException("Transitive closures can only take binary relations");
		}
		this.polarity = polarity;
	}

	public CallAtom(CallPolarity polarity, T target, Variable... substitution) {
		this(polarity, target, List.of(substitution));
	}

	public CallAtom(boolean positive, T target, List<Variable> substitution) {
		this(CallPolarity.fromBoolean(positive), target, substitution);
	}

	public CallAtom(boolean positive, T target, Variable... substitution) {
		this(positive, target, List.of(substitution));
	}

	public CallAtom(T target, List<Variable> substitution) {
		this(true, target, substitution);
	}

	public CallAtom(T target, Variable... substitution) {
		this(target, List.of(substitution));
	}

	public CallPolarity getPolarity() {
		return polarity;
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		if (polarity.isPositive()) {
			super.collectAllVariables(variables);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CallAtom<?> that = (CallAtom<?>) o;
		return Objects.equals(polarity, that.polarity)
				&& Objects.equals(getTarget(), that.getTarget())
				&& Objects.equals(getSubstitution(), that.getSubstitution());
	}

	@Override
	public int hashCode() {
		return Objects.hash(polarity, getTarget(), getSubstitution());
	}
}
