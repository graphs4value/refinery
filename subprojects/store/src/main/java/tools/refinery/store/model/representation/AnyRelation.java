package tools.refinery.store.model.representation;

import tools.refinery.store.model.RelationLike;
import tools.refinery.store.tuple.Tuple;

public sealed interface AnyRelation extends AnyDataRepresentation, RelationLike permits Relation {
	@Override
	Class<Tuple> getKeyType();
}
