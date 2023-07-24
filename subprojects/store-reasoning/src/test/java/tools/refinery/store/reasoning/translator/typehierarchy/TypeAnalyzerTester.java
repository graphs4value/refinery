/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;

class TypeAnalyzerTester {
	private final TypeAnalyzer sut;

	public TypeAnalyzerTester(TypeAnalyzer sut) {
		this.sut = sut;
	}

	public void assertAbstractType(PartialRelation partialRelation, PartialRelation... directSubtypes) {
		assertPreservedType(partialRelation, true, false, directSubtypes);
	}

	public void assertVacuousType(PartialRelation partialRelation) {
		assertPreservedType(partialRelation, true, true);
	}

	public void assertConcreteType(PartialRelation partialRelation, PartialRelation... directSubtypes) {
		assertPreservedType(partialRelation, false, false, directSubtypes);
	}

	private void assertPreservedType(PartialRelation partialRelation, boolean isAbstract, boolean isVacuous,
									 PartialRelation... directSubtypes) {
		var result = sut.getAnalysisResults().get(partialRelation);
		assertThat(result, is(instanceOf(PreservedType.class)));
		var preservedResult = (PreservedType) result;
		assertThat(preservedResult.isAbstractType(), is(isAbstract));
		assertThat(preservedResult.isVacuous(), is(isVacuous));
		assertThat(preservedResult.getDirectSubtypes(), hasItems(directSubtypes));
	}

	public void assertEliminatedType(PartialRelation partialRelation, PartialRelation replacement) {
		var result = sut.getAnalysisResults().get(partialRelation);
		assertThat(result, is(instanceOf(EliminatedType.class)));
		assertThat(((EliminatedType) result).replacement(), is(replacement));
	}

	public PreservedType getPreservedType(PartialRelation partialRelation) {
		return (PreservedType) sut.getAnalysisResults().get(partialRelation);
	}

	public InferredType getInferredType(PartialRelation partialRelation) {
		return getPreservedType(partialRelation).asInferredType();
	}
}
