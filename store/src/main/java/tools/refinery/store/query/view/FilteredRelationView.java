package tools.refinery.store.query.view;

import java.util.Objects;
import java.util.function.BiPredicate;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.Tuple.Tuple1;
import tools.refinery.store.model.representation.Relation;

public class FilteredRelationView<D> extends RelationView<D>{
	private final BiPredicate<Tuple,D> predicate;

	public FilteredRelationView(Relation<D> representation, BiPredicate<Tuple,D> predicate) {
		super(representation);
		this.predicate = predicate;
	}
	@Override
	protected Object[] forwardMap(Tuple key, D value) {
		return toTuple1Array(key);
	}
	@Override
	public boolean get(Model model, Object[] tuple) {
		int[] content = new int[tuple.length];
		for(int i = 0; i<tuple.length; i++) {
			content[i] =((Tuple1)tuple[i]).get(0);
		}
		Tuple key = Tuple.of(content);
		D value = model.get(representation, key);
		return filter(key, value);
	}
	
	public static Object[] toTuple1Array(Tuple t) {
		Object[] result = new Object[t.getSize()];
		for(int i = 0; i<t.getSize(); i++) {
			result[i] = Tuple.of(t.get(i));
		}
		return result;
	}
	
	@Override
	public int getArity() {
		return this.representation.getArity();
	}
	@Override
	protected boolean filter(Tuple key, D value) {
		return this.predicate.test(key, value);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(predicate);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FilteredRelationView other = (FilteredRelationView) obj;
		return Objects.equals(predicate, other.predicate);
	}
}
