/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.objectives;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStoreBuilder;

public interface Objective {
	default void doConfigure(ModelStoreBuilder storeBuilder) {
	}
	ObjectiveCalculator createCalculator(Model model);
}
