/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStoreBuilder;

@FunctionalInterface
public interface Propagator {
	default void configure(ModelStoreBuilder storeBuilder) {
	}

	BoundPropagator bindToModel(Model model);
}
