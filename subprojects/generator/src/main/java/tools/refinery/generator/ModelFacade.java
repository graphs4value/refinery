/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.TranslationException;

public abstract class ModelFacade {
	private final ProblemTrace problemTrace;
	private final ModelStore store;
	private final Model model;
	private final ReasoningAdapter reasoningAdapter;
	private final Concreteness concreteness;

	protected ModelFacade(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed,
                          Concreteness concreteness) {
		this.problemTrace = problemTrace;
		this.store = store;
		try {
			model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		} catch (TranslationException e) {
			throw problemTrace.wrapException(e);
		}
		reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		this.concreteness = concreteness;
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

	public Concreteness getConcreteness() {
		return concreteness;
	}

	public <A, C> PartialInterpretation<A, C> getPartialInterpretation(PartialSymbol<A, C> partialSymbol) {
		return reasoningAdapter.getPartialInterpretation(concreteness, partialSymbol);
	}
}
