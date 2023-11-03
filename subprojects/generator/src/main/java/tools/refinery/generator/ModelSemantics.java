/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.representation.TruthValue;

public class ModelSemantics extends AbstractRefinery {
	public ModelSemantics(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed) {
		super(problemTrace, store, modelSeed);
	}

	public <A, C> PartialInterpretation<A, C> getPartialInterpretation(PartialSymbol<A, C> partialSymbol) {
		return reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, partialSymbol);
	}

	public PartialInterpretation<TruthValue, Boolean> getPartialInterpretation(Relation relation) {
		return getPartialInterpretation(problemTrace.getPartialRelation(relation));
	}

	public PartialInterpretation<TruthValue, Boolean> getPartialInterpretation(QualifiedName qualifiedName) {
		return getPartialInterpretation(problemTrace.getPartialRelation(qualifiedName));
	}

	public PartialInterpretation<TruthValue, Boolean> getPartialInterpretation(String qualifiedName) {
		return getPartialInterpretation(problemTrace.getPartialRelation(qualifiedName));
	}

	public static ModelSemanticsBuilder standaloneBuilder() {
		var injector = StandaloneInjectorHolder.getInjector();
		return injector.getInstance(ModelSemanticsBuilder.class);
	}
}
