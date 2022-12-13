package tools.refinery.store.model.representation;

public sealed interface AnyDataRepresentation permits DataRepresentation, AnyRelation, AnyAuxiliaryData {
	String getName();

	Class<?> getKeyType();

	Class<?> getValueType();
}
