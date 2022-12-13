package tools.refinery.store.query.view;

import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.model.representation.Relation;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class FilteredRelationView<D> extends AbstractFilteredRelationView<D> {
	private final BiPredicate<Tuple, D> predicate;

	public FilteredRelationView(Relation<D> representation, String name, BiPredicate<Tuple, D> predicate) {
		super(representation, name);
		this.predicate = predicate;
	}

	public FilteredRelationView(Relation<D> representation, BiPredicate<Tuple, D> predicate) {
		super(representation);
		this.predicate = predicate;
	}

	public FilteredRelationView(Relation<D> representation, String name, Predicate<D> predicate) {
		this(representation, name, (k, v) -> predicate.test(v));
	}

	public FilteredRelationView(Relation<D> representation, Predicate<D> predicate) {
		this(representation, (k, v) -> predicate.test(v));
	}

	@Override
	public boolean filter(Tuple key, D value) {
		return this.predicate.test(key, value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		FilteredRelationView<?> that = (FilteredRelationView<?>) o;
		return Objects.equals(predicate, that.predicate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), predicate);
	}
}
