package tools.refinery.store.query.view;

import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;
import tools.refinery.store.representation.Symbol;

public class FunctionalRelationView<T> extends RelationView<T> {
	public FunctionalRelationView(Symbol<T> symbol, String name) {
		super(symbol, name);
	}

	public FunctionalRelationView(Symbol<T> symbol) {
		super(symbol);
	}

	@Override
	public boolean filter(Tuple key, T value) {
		return true;
	}

	@Override
	public Object[] forwardMap(Tuple key, T value) {
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
		T valueInTuple = (T) tuple[tuple.length - 1];
		T valueInMap = model.getInterpretation(getSymbol()).get(key);
		return valueInTuple.equals(valueInMap);
	}

	@Override
	public int arity() {
		return getSymbol().arity() + 1;
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
