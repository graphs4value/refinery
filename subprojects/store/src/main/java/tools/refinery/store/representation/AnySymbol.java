package tools.refinery.store.representation;

public sealed interface AnySymbol extends SymbolLike permits Symbol {
	Class<?> valueType();
}
