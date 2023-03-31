package tools.refinery.store.query.view;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.term.NodeSort;
import tools.refinery.store.query.term.Sort;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;
import tools.refinery.store.representation.Symbol;

import java.util.Arrays;
import java.util.List;

public abstract class TuplePreservingRelationView<T> extends RelationView<T> {
	protected TuplePreservingRelationView(Symbol<T> symbol, String name) {
		super(symbol, name);
	}

	protected TuplePreservingRelationView(Symbol<T> symbol) {
		super(symbol);
	}

	public Object[] forwardMap(Tuple key) {
		Object[] result = new Object[key.getSize()];
		for (int i = 0; i < key.getSize(); i++) {
			result[i] = Tuple.of(key.get(i));
		}
		return result;
	}

	@Override
	public Object[] forwardMap(Tuple key, T value) {
		return forwardMap(key);
	}

	@Override
	public boolean get(Model model, Object[] tuple) {
		int[] content = new int[tuple.length];
		for (int i = 0; i < tuple.length; i++) {
			content[i] = ((Tuple1) tuple[i]).value0();
		}
		Tuple key = Tuple.of(content);
		T value = model.getInterpretation(getSymbol()).get(key);
		return filter(key, value);
	}

	@Override
	public int arity() {
		return this.getSymbol().arity();
	}

	@Override
	public List<Sort> getSorts() {
		var sorts = new Sort[arity()];
		Arrays.fill(sorts, NodeSort.INSTANCE);
		return List.of(sorts);
	}
}
