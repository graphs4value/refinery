/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;

public sealed interface AnyPartialSymbolTranslator extends ModelStoreConfiguration permits PartialSymbolTranslator {
	AnyPartialSymbol getPartialSymbol();

	void configure(ModelStoreBuilder storeBuilder);
}
