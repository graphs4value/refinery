package tools.refinery.store.query.atom;

import tools.refinery.store.model.RelationLike;
import tools.refinery.store.query.Variable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CallAtom<T extends RelationLike> extends AbstractSubstitutionAtom<T> {
	private final CallKind kind;

	public CallAtom(CallKind kind, T target, List<Variable> substitution) {
		super(target, substitution);
		if (kind.isTransitive() && target.getArity() != 2) {
			throw new IllegalArgumentException("Transitive closures can only take binary relations");
		}
		this.kind = kind;
	}

	public CallAtom(CallKind kind, T target, Variable... substitution) {
		this(kind, target, List.of(substitution));
	}

	public CallAtom(boolean positive, T target, List<Variable> substitution) {
		this(CallKind.fromBoolean(positive), target, substitution);
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

	public CallKind getKind() {
		return kind;
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		if (kind.isPositive()) {
			super.collectAllVariables(variables);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CallAtom<?> that = (CallAtom<?>) o;
		return Objects.equals(kind, that.kind)
				&& Objects.equals(getTarget(), that.getTarget())
				&& Objects.equals(getSubstitution(), that.getSubstitution());
	}

	@Override
	public int hashCode() {
		return Objects.hash(kind, getTarget(), getSubstitution());
	}
}
