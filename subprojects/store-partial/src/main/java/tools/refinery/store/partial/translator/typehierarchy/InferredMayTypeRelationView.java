package tools.refinery.store.partial.translator.typehierarchy;

import tools.refinery.store.partial.representation.PartialRelation;
import tools.refinery.store.query.view.TuplePreservingRelationView;
import tools.refinery.store.tuple.Tuple;

class InferredMayTypeRelationView extends TuplePreservingRelationView<InferredType> {
	private final PartialRelation type;

	InferredMayTypeRelationView(PartialRelation type) {
		super(TypeHierarchyTranslationUnit.INFERRED_TYPE_SYMBOL, "%s#may".formatted(type));
		this.type = type;
	}

	@Override
	public boolean filter(Tuple key, InferredType value) {
		return value.mayConcreteTypes().contains(type);
	}
}
