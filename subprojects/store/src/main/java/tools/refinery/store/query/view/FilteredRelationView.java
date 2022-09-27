package tools.refinery.store.query.view;

import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.model.representation.Relation;

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
}
