package tools.refinery.store.model;

import tools.refinery.store.adapter.ModelAdapterType;
import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.model.internal.ModelStoreBuilderImpl;
import tools.refinery.store.representation.AnySymbol;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ModelStore {
	Collection<AnySymbol> getSymbols();

	Model createEmptyModel();

	Model createModelForState(long state);

	Set<Long> getStates();

	ModelDiffCursor getDiffCursor(long from, long to);

	<T extends ModelStoreAdapter> Optional<T> tryGetAdapter(ModelAdapterType<?, ? extends T, ?> adapterType);

	<T extends ModelStoreAdapter> T getAdapter(ModelAdapterType<?, T, ?> adapterType);

	static ModelStoreBuilder builder() {
		return new ModelStoreBuilderImpl();
	}
}
