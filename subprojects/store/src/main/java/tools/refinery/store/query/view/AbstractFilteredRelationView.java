package tools.refinery.store.query.view;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.Tuple.Tuple1;
import tools.refinery.store.model.representation.Relation;

public abstract class AbstractFilteredRelationView<D> extends RelationView<D> {
	protected AbstractFilteredRelationView(Relation<D> representation, String name) {
		super(representation, name);
	}

	protected AbstractFilteredRelationView(Relation<D> representation) {
		super(representation);
	}

	@Override
	public Object[] forwardMap(Tuple key, D value) {
		return toTuple1Array(key);
	}

	@Override
	public boolean get(Model model, Object[] tuple) {
		int[] content = new int[tuple.length];
		for (int i = 0; i < tuple.length; i++) {
			content[i] = ((Tuple1) tuple[i]).get(0);
		}
		Tuple key = Tuple.of(content);
		D value = model.get(getRepresentation(), key);
		return filter(key, value);
	}

	public int getArity() {
		return this.getRepresentation().getArity();
	}

	private static Object[] toTuple1Array(Tuple t) {
		Object[] result = new Object[t.getSize()];
		for (int i = 0; i < t.getSize(); i++) {
			result[i] = Tuple.of(t.get(i));
		}
		return result;
	}
}
