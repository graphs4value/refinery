/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.internal.ModelStoreBuilderImpl;
import tools.refinery.store.representation.AnySymbol;

import java.util.Collection;
import java.util.Optional;

public interface ModelStore {
	Collection<AnySymbol> getSymbols();

	Model createEmptyModel();

	Model createModelForState(Version state);

	ModelDiffCursor getDiffCursor(Version from, Version to);

	<T extends ModelStoreAdapter> Optional<T> tryGetAdapter(Class<? extends T> adapterType);

	<T extends ModelStoreAdapter> T getAdapter(Class<T> adapterType);

	void checkCancelled();

	static ModelStoreBuilder builder() {
		return new ModelStoreBuilderImpl();
	}
}
