/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.objectives;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;

public interface Objective {
	default void configure(ModelStoreBuilder storeBuilder) {
	}

	// The name {@code isAlwaysZero} is more straightforward than something like {@code canBeNonZero}.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	default boolean isAlwaysZero(ModelStore store) {
		return false;
	}

	ObjectiveCalculator createCalculator(Model model);
}
