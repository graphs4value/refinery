package tools.refinery.store.query.view;

import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.model.representation.Relation;

public class KeyOnlyRelationView extends AbstractFilteredRelationView<Boolean> {
	public static final String VIEW_NAME = "key";

	private final Boolean defaultValue;

	public KeyOnlyRelationView(Relation<Boolean> representation) {
		super(representation, VIEW_NAME);
		defaultValue = representation.getDefaultValue();
	}

	@Override
	public boolean filter(Tuple key, Boolean value) {
		return !value.equals(defaultValue);
	}
}
