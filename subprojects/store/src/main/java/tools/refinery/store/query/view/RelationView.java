package tools.refinery.store.query.view;

import tools.refinery.store.map.CursorAsIterator;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.RelationLike;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.model.representation.Relation;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a view of a {@link Relation} that can be queried.
 *
 * @param <D>
 * @author Oszkar Semerath
 */
public abstract class RelationView<D> implements RelationLike {
	private final Relation<D> representation;

	private final String name;

	protected RelationView(Relation<D> representation, String name) {
		this.representation = representation;
		this.name = name;
	}

	protected RelationView(Relation<D> representation) {
		this(representation, UUID.randomUUID().toString());
	}

	public Relation<D> getRepresentation() {
		return representation;
	}

	@Override
	public String getName() {
		return representation.getName() + "#" + name;
	}

	public abstract boolean filter(Tuple key, D value);

	public abstract Object[] forwardMap(Tuple key, D value);

	public abstract boolean get(Model model, Object[] tuple);

	public Iterable<Object[]> getAll(Model model) {
		return (() -> new CursorAsIterator<>(model.getAll(representation), this::forwardMap, this::filter));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hash(representation);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof RelationView))
			return false;
		@SuppressWarnings("unchecked")
		RelationView<D> other = ((RelationView<D>) obj);
		return Objects.equals(representation, other.representation);
	}
}
