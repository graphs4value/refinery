/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.tuple.Tuple;

public sealed interface AnyPartialInterpretationRefiner permits PartialInterpretationRefiner {
	ReasoningAdapter getAdapter();

	AnyPartialSymbol getPartialSymbol();

	/**
	 * Called after all {@link PartialInterpretationRefiner} instances have been created for symbols registered to
	 * the {@link tools.refinery.store.reasoning.ReasoningStoreAdapter}.
	 * <p>
	 * Override this method to access other {@link PartialInterpretationRefiner} instances associated to the
	 * {@link ReasoningAdapter} that are required for propagations executed by this
	 * {@link PartialInterpretationRefiner}.
	 */
	default void afterCreate() {
	}

	/**
	 * Execute propagations based on the contents of the {@code modelSeed} that would by executed if the
	 * {@code modelSeed} were written to the model as a sequence of
	 * {@link PartialInterpretationRefiner#merge(Tuple, AbstractValue)} calls.
	 * <p>
	 * This method is called only after {@link PartialModelInitializer#initialize(Model, ModelSeed)} was called on all
	 * {@link PartialModelInitializer} instances registered to the
	 * {@link tools.refinery.store.reasoning.ReasoningStoreAdapter}.
	 * <p>
	 * The default implementation of this method performs no actions. Override it make the behavior consistent with
	 * your {@link PartialInterpretationRefiner#merge(Tuple, AbstractValue)} implementation.
	 *
	 * @param modelSeed The model seed which was written by a previous call of
	 *                  {@link PartialModelInitializer#initialize(Model, ModelSeed)}.
	 */
	default void afterInitialize(ModelSeed modelSeed) {
	}
}
