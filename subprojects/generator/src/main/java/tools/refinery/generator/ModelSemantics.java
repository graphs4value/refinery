/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.seed.ModelSeed;

public class ModelSemantics extends ModelFacade {
	ModelSemantics(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed) {
		super(problemTrace, store, modelSeed, Concreteness.PARTIAL);
	}
}
