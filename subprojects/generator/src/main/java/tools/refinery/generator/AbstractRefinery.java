/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.seed.ModelSeed;

public class AbstractRefinery {
	protected final ProblemTrace problemTrace;
	protected final ModelStore store;
	protected final Model model;
	protected final ReasoningAdapter reasoningAdapter;

	public AbstractRefinery(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed) {
		this.problemTrace = problemTrace;
		this.store = store;
		model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
	}

	public ProblemTrace getProblemTrace() {
		return problemTrace;
	}

	public ModelStore getModelStore() {
		return store;
	}

	public Model getModel() {
		return model;
	}
}
