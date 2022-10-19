package tools.refinery.store.query.atom;

import tools.refinery.store.query.Variable;
import tools.refinery.store.query.view.RelationView;

import java.util.List;
import java.util.Objects;

public final class RelationViewAtom extends AbstractSubstitutionAtom<RelationView<?>> {
	public RelationViewAtom(RelationView<?> target, List<Variable> substitution) {
		super(target, substitution);
	}

	public RelationViewAtom(RelationView<?> target, Variable... substitution) {
		this(target, List.of(substitution));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RelationViewAtom relationViewAtom = (RelationViewAtom) o;
		return Objects.equals(getTarget(), relationViewAtom.getTarget())
				&& Objects.equals(getSubstitution(), relationViewAtom.getSubstitution());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getTarget(), getSubstitution());
	}
}
