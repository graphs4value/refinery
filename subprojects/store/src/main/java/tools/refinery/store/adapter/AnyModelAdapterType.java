package tools.refinery.store.adapter;

import java.util.Collection;

public sealed interface AnyModelAdapterType permits ModelAdapterType {
	Class<? extends ModelAdapter> getModelAdapterClass();

	Class<? extends ModelStoreAdapter> getModelStoreAdapterClass();

	Class<? extends ModelAdapterBuilder> getModelAdapterBuilderClass();

	Collection<AnyModelAdapterType> getSupportedAdapterTypes();

	default boolean supports(AnyModelAdapterType targetAdapter) {
		return getSupportedAdapterTypes().contains(targetAdapter);
	}

	String getName();
}
