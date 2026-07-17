/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import com.google.inject.Provider;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.SolutionSerializer;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;

public class ConcreteModelFacade extends ModelFacadeImpl {
	private final Provider<SolutionSerializer> solutionSerializerProvider;
	private final boolean keepNonExistingObjects;
	private final PartialInterpretation<TruthValue, Boolean> existsInterpretation;

	protected ConcreteModelFacade(Args args) {
		super(args.facadeArgs());
		solutionSerializerProvider = args.solutionSerializerProvider();
		keepNonExistingObjects = args.keepNonExistingObjects();
		existsInterpretation = keepNonExistingObjects ? null :
				super.getPartialInterpretation(ReasoningAdapter.EXISTS_SYMBOL);
	}

	@Override
	public Concreteness getConcreteness() {
		return Concreteness.CANDIDATE;
	}

	@Override
	public <A extends AbstractValue<A, C>, C> PartialInterpretation<A, C> getPartialInterpretation(
			PartialSymbol<A, C> partialSymbol) {
		var partialInterpretation = super.getPartialInterpretation(partialSymbol);
		if (keepNonExistingObjects || ReasoningAdapter.EXISTS_SYMBOL.equals(partialSymbol)) {
			return partialInterpretation;
		}
		return FilteredInterpretation.of(partialInterpretation, existsInterpretation);
	}

	@Override
	public Problem serialize() {
		var serializer = getSolutionSerializer();
		return serializer.serializeSolution(getProblemTrace(), getModel());
	}

	protected SolutionSerializer getSolutionSerializer() {
		return solutionSerializerProvider.get();
	}

	public record Args(ModelFacadeImpl.Args facadeArgs, Provider<SolutionSerializer> solutionSerializerProvider,
					   boolean keepNonExistingObjects) {
	}
}
