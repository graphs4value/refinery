package tools.refinery.store.representation;

public interface SymbolLike {
	String name();

	int arity();

	default boolean invalidIndex(int i) {
		return i < 0 || i >= arity();
	}
}
