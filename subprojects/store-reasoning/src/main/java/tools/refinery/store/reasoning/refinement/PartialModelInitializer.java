/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.seed.ModelSeed;

@FunctionalInterface
public interface PartialModelInitializer {
	void initialize(Model model, ModelSeed modelSeed);
}
