package tools.refinery.store.query.view;

import tools.refinery.store.map.CursorAsIterator;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a view of a {@link Relation} that can be queried.
 *
 * @param <D>
 * @author Oszkar Semerath
 */
public abstract non-sealed class RelationView<D> implements AnyRelationView {
	private final Relation<D> representation;

	private final String name;

	protected RelationView(Relation<D> representation, String name) {
		this.representation = representation;
		this.name = name;
	}

	protected RelationView(Relation<D> representation) {
		this(representation, UUID.randomUUID().toString());
	}

	@Override
	public Relation<D> getRepresentation() {
		return representation;
	}

	@Override
	public String getName() {
		return representation.getName() + "#" + name;
	}

	public abstract boolean filter(Tuple key, D value);

	public abstract Object[] forwardMap(Tuple key, D value);

	@Override
	public Iterable<Object[]> getAll(Model model) {
		return (() -> new CursorAsIterator<>(model.getAll(representation), this::forwardMap, this::filter));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RelationView<?> that = (RelationView<?>) o;
		return Objects.equals(representation, that.representation) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(representation, name);
	}
}
