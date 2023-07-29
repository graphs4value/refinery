/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.AnySymbol;

import java.util.Collection;
import java.util.List;

public interface StateCoderBuilder extends ModelAdapterBuilder {
	StateCoderBuilder exclude(AnySymbol symbol);
	default StateCoderBuilder excludeAll(Collection<? extends AnySymbol> symbols) {
		for(var symbol : symbols) {
			exclude(symbol);
		}
		return this;
	}
	default StateCoderBuilder excludeAll(AnySymbol... symbols) {
		return excludeAll(List.of(symbols));
	}

	@Override
	StateCoderStoreAdapter build(ModelStore store);
}
