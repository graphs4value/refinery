package tools.refinery.store.query.view;

import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;
import tools.refinery.store.model.representation.Relation;

public class FunctionalRelationView<D> extends RelationView<D> {
	public FunctionalRelationView(Relation<D> representation, String name) {
		super(representation, name);
	}

	public FunctionalRelationView(Relation<D> representation) {
		super(representation);
	}

	@Override
	public boolean filter(Tuple key, D value) {
		return true;
	}

	@Override
	public Object[] forwardMap(Tuple key, D value) {
		return toTuple1ArrayPlusValue(key, value);
	}

	@Override
	public boolean get(Model model, Object[] tuple) {
		int[] content = new int[tuple.length - 1];
		for (int i = 0; i < tuple.length - 1; i++) {
			content[i] = ((Tuple1) tuple[i]).value0();
		}
		Tuple key = Tuple.of(content);
		@SuppressWarnings("unchecked")
		D valueInTuple = (D) tuple[tuple.length - 1];
		D valueInMap = model.get(getRepresentation(), key);
		return valueInTuple.equals(valueInMap);
	}

	@Override
	public int getArity() {
		return getRepresentation().getArity() + 1;
	}

	private static <D> Object[] toTuple1ArrayPlusValue(Tuple t, D value) {
		Object[] result = new Object[t.getSize() + 1];
		for (int i = 0; i < t.getSize(); i++) {
			result[i] = Tuple.of(t.get(i));
		}
		result[t.getSize()] = value;
		return result;
	}
}
