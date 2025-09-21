/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.generator.impl.ConcreteModelSemantics;
import tools.refinery.generator.impl.ModelSemanticsImpl;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.translator.TranslationException;

import java.util.Collection;
import java.util.Set;

// This class is used as a fluent builder, so it's not necessary to use the return value of all of its methods.
@SuppressWarnings("UnusedReturnValue")
public final class ModelSemanticsFactory extends ModelFacadeFactory<ModelSemanticsFactory> {
	private boolean withCandidateInterpretations;
	private boolean concretize;

	@Override
	protected ModelSemanticsFactory getSelf() {
		return this;
	}

	public ModelSemanticsFactory withCandidateInterpretations(boolean withCandidateInterpretations) {
		this.withCandidateInterpretations = withCandidateInterpretations;
		return this;
	}

	public ModelSemanticsFactory concretize(boolean concretize) {
		this.concretize = concretize;
		return this;
	}

	public ModelSemantics createSemantics(Problem problem) {
		var semantics = tryCreateSemantics(problem);
		semantics.throwIfInitializationFailed();
		return semantics;
	}

	public ModelSemantics tryCreateSemantics(Problem problem) {
		var initializer = createModelInitializer();
		try {
			initializer.readProblem(problem);
		} catch (TracedException e) {
			throw getDiagnostics().wrapTracedException(e, problem);
		}
		checkCancelled();
		var storeBuilder = ModelStore.builder()
				.cancellationToken(getCancellationToken())
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder()
						.throwOnFatalRejection(false))
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(getRequiredInterpretations()));
		try {
			initializer.configureStoreBuilder(storeBuilder);
		} catch (TranslationException e) {
			throw getDiagnostics().wrapTranslationException(e, initializer.getProblemTrace());
		} catch (TracedException e) {
			throw getDiagnostics().wrapTracedException(e, problem);
		}
		if (concretize) {
			return new ConcreteModelSemantics(createConcreteFacadeArgs(initializer, storeBuilder));
		}
		return new ModelSemanticsImpl(createFacadeArgs(initializer, storeBuilder));
	}

	private Collection<Concreteness> getRequiredInterpretations() {
		if (concretize) {
			return Set.of(Concreteness.CANDIDATE);
		}
		if (withCandidateInterpretations) {
			return Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE);
		}
		return Set.of(Concreteness.PARTIAL);
	}
}
