package tools.refinery.store.query.atom;

import tools.refinery.store.query.Variable;
import tools.refinery.store.representation.SymbolLike;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class CallAtom<T extends SymbolLike> implements DNFAtom {
	private final CallPolarity polarity;
	private final T target;
	private final List<Variable> substitution;

	protected CallAtom(CallPolarity polarity, T target, List<Variable> substitution) {
		if (substitution.size() != target.arity()) {
			throw new IllegalArgumentException("%s needs %d arguments, but got %s".formatted(target.name(),
					target.arity(), substitution.size()));
		}
		if (polarity.isTransitive() && target.arity() != 2) {
			throw new IllegalArgumentException("Transitive closures can only take binary relations");
		}
		this.polarity = polarity;
		this.target = target;
		this.substitution = substitution;
	}

	protected CallAtom(CallPolarity polarity, T target, Variable... substitution) {
		this(polarity, target, List.of(substitution));
	}

	protected CallAtom(boolean positive, T target, List<Variable> substitution) {
		this(CallPolarity.fromBoolean(positive), target, substitution);
	}

	protected CallAtom(boolean positive, T target, Variable... substitution) {
		this(positive, target, List.of(substitution));
	}

	protected CallAtom(T target, List<Variable> substitution) {
		this(true, target, substitution);
	}

	protected CallAtom(T target, Variable... substitution) {
		this(target, List.of(substitution));
	}

	public CallPolarity getPolarity() {
		return polarity;
	}

	public T getTarget() {
		return target;
	}

	public List<Variable> getSubstitution() {
		return substitution;
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		if (polarity.isPositive()) {
			variables.addAll(substitution);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CallAtom<?> callAtom = (CallAtom<?>) o;
		return polarity == callAtom.polarity && Objects.equals(target, callAtom.target) && Objects.equals(substitution
				, callAtom.substitution);
	}

	@Override
	public int hashCode() {
		return Objects.hash(polarity, target, substitution);
	}
}
