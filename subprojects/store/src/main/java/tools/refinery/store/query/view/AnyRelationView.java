package tools.refinery.store.query.view;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.RelationLike;
import tools.refinery.store.model.representation.AnyRelation;

public sealed interface AnyRelationView extends RelationLike permits RelationView {
	AnyRelation getRepresentation();

	boolean get(Model model, Object[] tuple);

	Iterable<Object[]> getAll(Model model);
}
