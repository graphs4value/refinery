/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import tools.refinery.generator.ModelFacade;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.seed.PropagatedModel;
import tools.refinery.store.reasoning.translator.TranslationException;

public abstract class ModelFacadeImpl implements ModelFacade {
	private final ProblemTrace problemTrace;
	private final ModelStore store;
	private final PropagationResult propagationResult;
	private final Model model;
	private final ReasoningAdapter reasoningAdapter;

	protected ModelFacadeImpl(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed) {
		this.problemTrace = problemTrace;
		this.store = store;
		PropagatedModel propagatedModel;
		try {
			propagatedModel = store.getAdapter(ReasoningStoreAdapter.class).tryCreateInitialModel(modelSeed);
		} catch (TranslationException e) {
			throw problemTrace.wrapException(e);
		}
		model = propagatedModel.model();
		propagationResult = afterPropagation(propagatedModel.propagationResult());
		reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
	}

	@Override
	public ProblemTrace getProblemTrace() {
		return problemTrace;
	}

	@Override
	public ModelStore getModelStore() {
		return store;
	}

	@Override
	public Model getModel() {
		return model;
	}

	protected PropagationResult afterPropagation(PropagationResult createInitialModelResult) {
		return createInitialModelResult;
	}

	@Override
	public PropagationResult getPropagationResult() {
		return propagationResult;
	}

	@Override
	public <A extends AbstractValue<A, C>, C> PartialInterpretation<A, C> getPartialInterpretation(
			PartialSymbol<A, C> partialSymbol) {
		return reasoningAdapter.getPartialInterpretation(getConcreteness(), partialSymbol);
	}
}
