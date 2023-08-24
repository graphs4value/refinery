package tools.refinery.store.dse.internal.action;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

import java.util.Arrays;

public class InsertAction<T> implements AtomicAction {

	private final Interpretation<T> interpretation;
	private final T value;

	private final ActionSymbol[] symbols;

	public InsertAction(Interpretation<T> interpretation, T value, ActionSymbol... symbols) {
		this.interpretation = interpretation;
		this.value = value;
		this.symbols = symbols;

	}

	@Override
	public void fire(Tuple activation) {
		var tuple = Tuple.of(Arrays.stream(symbols).map(symbol -> symbol.getValue(activation).get(0))
				.mapToInt(Integer::intValue).toArray());

		interpretation.put(tuple, value);
	}

	@Override
	public InsertAction<T> prepare(Model model) {
		return this;
	}

	public ActionSymbol[] getSymbols() {
		return symbols;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof InsertAction<?> other)) {
			return false;
		}
		if (symbols.length != other.symbols.length) {
			return false;
		}
		if (!interpretation.equals(other.interpretation)) {
			return false;
		}
		return value.equals(other.value);
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + Integer.hashCode(symbols.length);
		result = 31 * result + interpretation.hashCode();
		result = 31 * result + value.hashCode();
		return result;
	}
}
