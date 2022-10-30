package tools.refinery.store.query.atom;

import tools.refinery.store.model.RelationLike;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;

public record ModalRelation(Modality modality, Relation<TruthValue> relation) implements RelationLike {
	@Override
	public String getName() {
		return "%s %s".formatted(modality, relation);
	}

	@Override
	public int getArity() {
		return relation.getArity();
	}
}
