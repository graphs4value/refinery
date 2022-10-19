package tools.refinery.store.query.atom;

import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.query.Variable;

import java.util.List;
import java.util.Objects;

public final class RelationAtom extends AbstractCallAtom<Relation<?>> {
	public RelationAtom(CallKind kind, Relation<?> target, List<Variable> substitution) {
		super(kind, target, substitution);
	}

	public RelationAtom(Relation<?> target, List<Variable> substitution) {
		this(CallKind.POSITIVE, target, substitution);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RelationAtom relationAtom = (RelationAtom) o;
		return Objects.equals(getKind(), relationAtom.getKind())
				&& Objects.equals(getTarget(), relationAtom.getTarget())
				&& Objects.equals(getSubstitution(), relationAtom.getSubstitution());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getKind(), getTarget(), getSubstitution());
	}
}
