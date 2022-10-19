package tools.refinery.store.query.atom;

import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.query.Variable;

import java.util.List;
import java.util.Objects;

public final class ModalRelationAtom extends AbstractCallAtom<Relation<TruthValue>> {
	private final Modality modality;

	public ModalRelationAtom(CallKind kind, Modality modality, Relation<TruthValue> target,
							 List<Variable> substitution) {
		super(kind, target, substitution);
		this.modality = modality;
	}

	public ModalRelationAtom(Modality modality, Relation<TruthValue> target, List<Variable> substitution) {
		this(CallKind.POSITIVE, modality, target, substitution);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ModalRelationAtom modalRelationAtom = (ModalRelationAtom) o;
		return Objects.equals(getKind(), modalRelationAtom.getKind())
				&& modality == modalRelationAtom.modality
				&& Objects.equals(getTarget(), modalRelationAtom.getTarget())
				&& Objects.equals(getSubstitution(), modalRelationAtom.getSubstitution());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getKind(), modality, getTarget(), getSubstitution());
	}
}
