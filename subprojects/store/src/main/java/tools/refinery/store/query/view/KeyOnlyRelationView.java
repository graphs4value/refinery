package tools.refinery.store.query.view;

import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.model.representation.Relation;

import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		KeyOnlyRelationView that = (KeyOnlyRelationView) o;
		return Objects.equals(defaultValue, that.defaultValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), defaultValue);
	}
}
