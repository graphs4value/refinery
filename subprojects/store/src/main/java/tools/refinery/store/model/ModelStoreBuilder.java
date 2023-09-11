/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.util.CancellationToken;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ModelStoreBuilder {
	ModelStoreBuilder cancellationToken(CancellationToken cancellationToken);

	default ModelStoreBuilder symbols(AnySymbol... symbols) {
		return symbols(List.of(symbols));
	}

	default ModelStoreBuilder symbols(Collection<? extends AnySymbol> symbols) {
		symbols.forEach(this::symbol);
		return this;
	}

	default ModelStoreBuilder symbol(AnySymbol symbol) {
		return symbol((Symbol<?>) symbol);
	}

	<T> ModelStoreBuilder symbol(Symbol<T> symbol);

	ModelStoreBuilder with(ModelAdapterBuilder adapterBuilder);

	ModelStoreBuilder with(ModelStoreConfiguration configuration);

	<T extends ModelAdapterBuilder> Optional<T> tryGetAdapter(Class<? extends T> adapterType);

	<T extends ModelAdapterBuilder> T getAdapter(Class<T> adapterType);

	ModelStore build();
}
