/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.objectives;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.literal.Reduction;

public interface Criterion {
	default void configure(ModelStoreBuilder storeBuilder) {
	}

	default Reduction getReduction(ModelStore store) {
		return Reduction.NOT_REDUCIBLE;
	}

	CriterionCalculator createCalculator(Model model);
}
