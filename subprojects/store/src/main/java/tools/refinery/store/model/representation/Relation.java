package tools.refinery.store.model.representation;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.model.RelationLike;
import tools.refinery.store.model.TupleHashProvider;
import tools.refinery.store.tuple.Tuple;

public final class Relation<D> extends DataRepresentation<Tuple, D> implements RelationLike {
	private final int arity;

	public Relation(String name, int arity, Class<D> valueType, D defaultValue) {
		super(name, Tuple.class, valueType, defaultValue);
		this.arity = arity;
	}

	@Override
	public int getArity() {
		return arity;
	}

	@Override
	public ContinousHashProvider<Tuple> getHashProvider() {
		return TupleHashProvider.singleton();
	}

	@Override
	public boolean isValidKey(Tuple key) {
		if (key == null) {
			return false;
		} else {
			return key.getSize() == getArity();
		}
	}
}
