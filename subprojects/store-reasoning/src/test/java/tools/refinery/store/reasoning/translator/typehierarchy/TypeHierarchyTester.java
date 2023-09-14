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

class TypeHierarchyTester {
	private final TypeHierarchy sut;

	public TypeHierarchyTester(TypeHierarchy sut) {
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
		var result = sut.getPreservedTypes().get(partialRelation);
		assertThat(result, not(nullValue()));
		assertThat(result.isAbstractType(), is(isAbstract));
		assertThat(result.isVacuous(), is(isVacuous));
		assertThat(result.getDirectSubtypes(), hasItems(directSubtypes));
	}

	public void assertEliminatedType(PartialRelation partialRelation, PartialRelation replacement) {
		var result = sut.getEliminatedTypes().get(partialRelation);
		assertThat(result, not(nullValue()));
		assertThat(result, is(replacement));
	}

	public TypeAnalysisResult getPreservedType(PartialRelation partialRelation) {
		return sut.getPreservedTypes().get(partialRelation);
	}

	public InferredType getInferredType(PartialRelation partialRelation) {
		return getPreservedType(partialRelation).asInferredType();
	}
}
