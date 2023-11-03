/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.generator.impl.ProblemTraceImpl;
import tools.refinery.language.semantics.metadata.NodeMetadata;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.TranslationException;

import java.util.List;

public abstract class AbstractRefinery {
	protected final ProblemTrace problemTrace;
	protected final ModelStore store;
	protected final Model model;
	protected final ReasoningAdapter reasoningAdapter;

	protected AbstractRefinery(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed) {
		this.problemTrace = problemTrace;
		this.store = store;
		try {
			model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		} catch (TranslationException e) {
			throw ProblemTraceImpl.wrapException(problemTrace, e);
		}
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

	public List<NodeMetadata> getNodesMetadata() {
		int nodeCount = reasoningAdapter.getNodeCount();
		return problemTrace.getNodesMetadata(nodeCount, isPreserveNewNodes());
	}

	protected abstract boolean isPreserveNewNodes();
}
