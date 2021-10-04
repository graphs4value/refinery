package tools.refinery.data.query.view;

import tools.refinery.data.model.Tuple;
import tools.refinery.data.model.representation.Relation;

public class KeyOnlyRelationView extends FilteredRelationView<Boolean>{

	public KeyOnlyRelationView(Relation<Boolean> representation) {
		super(representation, (k,v)->true);
	}
	@Override
	protected boolean filter(Tuple key, Boolean value) {
		return true;
	}
	
}
