package tools.refinery.store.representation;

public sealed interface AnySymbol permits Symbol {
	String name();

	int arity();

	Class<?> valueType();
}
