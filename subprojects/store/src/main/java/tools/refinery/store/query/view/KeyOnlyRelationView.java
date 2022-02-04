package tools.refinery.store.query.view;

import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.representation.Relation;

public class KeyOnlyRelationView extends FilteredRelationView<Boolean>{

	public KeyOnlyRelationView(Relation<Boolean> representation) {
		super(representation, (k,v)->true);
	}
	@Override
	protected boolean filter(Tuple key, Boolean value) {
		return !value.equals(representation.getDefaultValue());
	}
	
}
