package tools.refinery.store.query.view;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.dnf.FunctionalDependency;
import tools.refinery.store.query.term.DataSort;
import tools.refinery.store.query.term.NodeSort;
import tools.refinery.store.query.term.Sort;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class FunctionalRelationView<T> extends RelationView<T> {
	private final T defaultValue;

	public FunctionalRelationView(Symbol<T> symbol, String name) {
		super(symbol, name);
		defaultValue = symbol.defaultValue();
	}

	public FunctionalRelationView(Symbol<T> symbol) {
		super(symbol);
		defaultValue = symbol.defaultValue();
	}

	@Override
	public Set<FunctionalDependency<Integer>> getFunctionalDependencies() {
		var arity = getSymbol().arity();
		var forEach = IntStream.range(0, arity).boxed().collect(Collectors.toUnmodifiableSet());
		var unique = Set.of(arity);
		return Set.of(new FunctionalDependency<>(forEach, unique));
	}

	@Override
	public Set<RelationViewImplication> getImpliedRelationViews() {
		var symbol = getSymbol();
		var impliedIndices = IntStream.range(0, symbol.arity()).boxed().toList();
		var keyOnlyRelationView = new KeyOnlyRelationView<>(symbol);
		return Set.of(new RelationViewImplication(this, keyOnlyRelationView, impliedIndices));
	}

	@Override
	public boolean filter(Tuple key, T value) {
		return !Objects.equals(defaultValue, value);
	}

	@Override
	public Object[] forwardMap(Tuple key, T value) {
		int size = key.getSize();
		Object[] result = new Object[size + 1];
		for (int i = 0; i < size; i++) {
			result[i] = Tuple.of(key.get(i));
		}
		result[key.getSize()] = value;
		return result;
	}

	@Override
	public boolean get(Model model, Object[] tuple) {
		int[] content = new int[tuple.length - 1];
		for (int i = 0; i < tuple.length - 1; i++) {
			content[i] = ((Tuple1) tuple[i]).value0();
		}
		Tuple key = Tuple.of(content);
		@SuppressWarnings("unchecked")
		T valueInTuple = (T) tuple[tuple.length - 1];
		T valueInMap = model.getInterpretation(getSymbol()).get(key);
		return valueInTuple.equals(valueInMap);
	}

	@Override
	public int arity() {
		return getSymbol().arity() + 1;
	}

	@Override
	public List<Sort> getSorts() {
		var sorts = new Sort[arity()];
		int valueIndex = sorts.length - 1;
		for (int i = 0; i < valueIndex; i++) {
			sorts[i] = NodeSort.INSTANCE;
		}
		sorts[valueIndex] = new DataSort<>(getSymbol().valueType());
		return List.of(sorts);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		FunctionalRelationView<?> that = (FunctionalRelationView<?>) o;
		return Objects.equals(defaultValue, that.defaultValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), defaultValue);
	}
}
