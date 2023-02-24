package tools.refinery.store.query.view;

import tools.refinery.store.query.Variable;
import tools.refinery.store.map.CursorAsIterator;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.RelationViewLiteral;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a view of a {@link Symbol} that can be queried.
 *
 * @param <T>
 * @author Oszkar Semerath
 */
public abstract non-sealed class RelationView<T> implements AnyRelationView {
	private final Symbol<T> symbol;

	private final String viewName;

	protected RelationView(Symbol<T> symbol, String viewName) {
		this.symbol = symbol;
		this.viewName = viewName;
	}

	protected RelationView(Symbol<T> representation) {
		this(representation, UUID.randomUUID().toString());
	}

	@Override
	public Symbol<T> getSymbol() {
		return symbol;
	}

	@Override
	public String getViewName() {
		return viewName;
	}

	@Override
	public String name() {
		return symbol.name() + "#" + viewName;
	}

	public abstract boolean filter(Tuple key, T value);

	public abstract Object[] forwardMap(Tuple key, T value);

	@Override
	public Iterable<Object[]> getAll(Model model) {
		return (() -> new CursorAsIterator<>(model.getInterpretation(symbol).getAll(), this::forwardMap, this::filter));
	}

	public RelationViewLiteral call(CallPolarity polarity, List<Variable> arguments) {
		return new RelationViewLiteral(polarity, this, arguments);
	}

	public RelationViewLiteral call(CallPolarity polarity, Variable... arguments) {
		return call(polarity, List.of(arguments));
	}

	public RelationViewLiteral call(Variable... arguments) {
		return call(CallPolarity.POSITIVE, arguments);
	}

	public RelationViewLiteral callTransitive(Variable left, Variable right) {
		return call(CallPolarity.TRANSITIVE, List.of(left, right));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RelationView<?> that = (RelationView<?>) o;
		return Objects.equals(symbol, that.symbol) && Objects.equals(viewName, that.viewName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(symbol, viewName);
	}
}
