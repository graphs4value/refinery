/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.TruthValue;

import java.util.LinkedHashMap;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypeAnalyzerTest {
	@Test
	void directSupertypesTest() {
		var c1 = new PartialRelation("C1", 1);
		var c2 = new PartialRelation("C2", 1);
		var c3 = new PartialRelation("C3", 1);
		var typeInfoMap = new LinkedHashMap<PartialRelation, TypeInfo>();
		typeInfoMap.put(c1, TypeInfo.builder().supertypes(c2, c3).build());
		typeInfoMap.put(c2, TypeInfo.builder().supertype(c3).build());
		typeInfoMap.put(c3, TypeInfo.builder().build());

		var sut = new TypeAnalyzer(typeInfoMap);
		var tester = new TypeAnalyzerTester(sut);

		assertAll(
				() -> tester.assertConcreteType(c1),
				() -> tester.assertConcreteType(c2, c1),
				() -> tester.assertConcreteType(c3, c2)
		);
	}

	@Test
	void typeEliminationAbstractToConcreteTest() {
		var c1 = new PartialRelation("C1", 1);
		var c2 = new PartialRelation("C2", 1);
		var a11 = new PartialRelation("A11", 1);
		var a12 = new PartialRelation("A12", 1);
		var a21 = new PartialRelation("A21", 1);
		var a22 = new PartialRelation("A22", 1);
		var a3 = new PartialRelation("A3", 1);
		var typeInfoMap = new LinkedHashMap<PartialRelation, TypeInfo>();
		typeInfoMap.put(a3, TypeInfo.builder().abstractType().build());
		typeInfoMap.put(a21, TypeInfo.builder().abstractType().supertype(a3).build());
		typeInfoMap.put(a22, TypeInfo.builder().abstractType().supertype(a3).build());
		typeInfoMap.put(a11, TypeInfo.builder().abstractType().supertypes(a21, a22).build());
		typeInfoMap.put(a12, TypeInfo.builder().abstractType().supertypes(a21, a22).build());
		typeInfoMap.put(c1, TypeInfo.builder().supertypes(a11, a12).build());
		typeInfoMap.put(c2, TypeInfo.builder().supertype(a3).build());

		var sut = new TypeAnalyzer(typeInfoMap);
		var tester = new TypeAnalyzerTester(sut);

		assertAll(
				() -> tester.assertConcreteType(c1),
				() -> tester.assertConcreteType(c2),
				() -> tester.assertEliminatedType(a11, c1),
				() -> tester.assertEliminatedType(a12, c1),
				() -> tester.assertEliminatedType(a21, c1),
				() -> tester.assertEliminatedType(a22, c1),
				() -> tester.assertAbstractType(a3, c1, c2)
		);
	}

	@Test
	void typeEliminationConcreteToAbstractTest() {
		var c1 = new PartialRelation("C1", 1);
		var c2 = new PartialRelation("C2", 1);
		var a11 = new PartialRelation("A11", 1);
		var a12 = new PartialRelation("A12", 1);
		var a21 = new PartialRelation("A21", 1);
		var a22 = new PartialRelation("A22", 1);
		var a3 = new PartialRelation("A3", 1);
		var typeInfoMap = new LinkedHashMap<PartialRelation, TypeInfo>();
		typeInfoMap.put(c1, TypeInfo.builder().supertypes(a11, a12).build());
		typeInfoMap.put(c2, TypeInfo.builder().supertype(a3).build());
		typeInfoMap.put(a11, TypeInfo.builder().abstractType().supertypes(a21, a22).build());
		typeInfoMap.put(a12, TypeInfo.builder().abstractType().supertypes(a21, a22).build());
		typeInfoMap.put(a21, TypeInfo.builder().abstractType().supertype(a3).build());
		typeInfoMap.put(a22, TypeInfo.builder().abstractType().supertype(a3).build());
		typeInfoMap.put(a3, TypeInfo.builder().abstractType().build());

		var sut = new TypeAnalyzer(typeInfoMap);
		var tester = new TypeAnalyzerTester(sut);

		assertAll(
				() -> tester.assertConcreteType(c1),
				() -> tester.assertConcreteType(c2),
				() -> tester.assertEliminatedType(a11, c1),
				() -> tester.assertEliminatedType(a12, c1),
				() -> tester.assertEliminatedType(a21, c1),
				() -> tester.assertEliminatedType(a22, c1),
				() -> tester.assertAbstractType(a3, c1, c2)
		);
	}

	@Test
	void preserveConcreteTypeTest() {
		var c1 = new PartialRelation("C1", 1);
		var a1 = new PartialRelation("A1", 1);
		var c2 = new PartialRelation("C2", 1);
		var a2 = new PartialRelation("A2", 1);
		var typeInfoMap = new LinkedHashMap<PartialRelation, TypeInfo>();
		typeInfoMap.put(c1, TypeInfo.builder().supertype(a1).build());
		typeInfoMap.put(a1, TypeInfo.builder().abstractType().supertype(c2).build());
		typeInfoMap.put(c2, TypeInfo.builder().supertype(a2).build());
		typeInfoMap.put(a2, TypeInfo.builder().abstractType().build());

		var sut = new TypeAnalyzer(typeInfoMap);
		var tester = new TypeAnalyzerTester(sut);

		assertAll(
				() -> tester.assertConcreteType(c1),
				() -> tester.assertEliminatedType(a1, c1),
				() -> tester.assertConcreteType(c2, c1),
				() -> tester.assertEliminatedType(a2, c2)
		);
	}

	@Test
	void mostGeneralCurrentTypeTest() {
		var c1 = new PartialRelation("C1", 1);
		var c2 = new PartialRelation("C2", 1);
		var c3 = new PartialRelation("C3", 1);
		var typeInfoMap = new LinkedHashMap<PartialRelation, TypeInfo>();
		typeInfoMap.put(c1, TypeInfo.builder().supertype(c3).build());
		typeInfoMap.put(c2, TypeInfo.builder().supertype(c3).build());
		typeInfoMap.put(c3, TypeInfo.builder().build());

		var sut = new TypeAnalyzer(typeInfoMap);
		var tester = new TypeAnalyzerTester(sut);
		var c3Result = tester.getPreservedType(c3);

		var expected = new InferredType(Set.of(c3), Set.of(c1, c2, c3), c3);
		assertAll(
				() -> assertThat(tester.getInferredType(c3), Matchers.is(expected)),
				() -> assertThat(c3Result.merge(sut.getUnknownType(), TruthValue.TRUE), Matchers.is(expected))
		);
	}

	@Test
	void preferFirstConcreteTypeTest() {
		var a1 = new PartialRelation("A1", 1);
		var c1 = new PartialRelation("C1", 1);
		var c2 = new PartialRelation("C2", 1);
		var c3 = new PartialRelation("C3", 1);
		var c4 = new PartialRelation("C4", 1);
		var typeInfoMap = new LinkedHashMap<PartialRelation, TypeInfo>();
		typeInfoMap.put(c1, TypeInfo.builder().supertype(a1).build());
		typeInfoMap.put(c2, TypeInfo.builder().supertype(a1).build());
		typeInfoMap.put(c3, TypeInfo.builder().supertype(a1).build());
		typeInfoMap.put(c4, TypeInfo.builder().supertype(c3).build());
		typeInfoMap.put(a1, TypeInfo.builder().abstractType().build());

		var sut = new TypeAnalyzer(typeInfoMap);
		var tester = new TypeAnalyzerTester(sut);
		var c1Result = tester.getPreservedType(c1);
		var a1Result = tester.getPreservedType(a1);

		assertThat(c1Result.merge(a1Result.asInferredType(), TruthValue.FALSE),
				Matchers.is(new InferredType(Set.of(a1), Set.of(c2, c3, c4), c2)));
	}

	@Test
	void preferFirstMostGeneralConcreteTypeTest() {
		var a1 = new PartialRelation("A1", 1);
		var c1 = new PartialRelation("C1", 1);
		var c2 = new PartialRelation("C2", 1);
		var c3 = new PartialRelation("C3", 1);
		var c4 = new PartialRelation("C4", 1);
		var typeInfoMap = new LinkedHashMap<PartialRelation, TypeInfo>();
		typeInfoMap.put(c4, TypeInfo.builder().supertype(c3).build());
		typeInfoMap.put(c3, TypeInfo.builder().supertype(a1).build());
		typeInfoMap.put(c2, TypeInfo.builder().supertype(a1).build());
		typeInfoMap.put(c1, TypeInfo.builder().supertype(a1).build());
		typeInfoMap.put(a1, TypeInfo.builder().abstractType().build());

		var sut = new TypeAnalyzer(typeInfoMap);
		var tester = new TypeAnalyzerTester(sut);
		var c1Result = tester.getPreservedType(c1);
		var a1Result = tester.getPreservedType(a1);

		assertThat(c1Result.merge(a1Result.asInferredType(), TruthValue.FALSE),
				Matchers.is(new InferredType(Set.of(a1), Set.of(c2, c3, c4), c3)));
	}

	@Test
	void circularTypeHierarchyTest() {
		var c1 = new PartialRelation("C1", 1);
		var c2 = new PartialRelation("C2", 1);
		var typeInfoMap = new LinkedHashMap<PartialRelation, TypeInfo>();
		typeInfoMap.put(c1, TypeInfo.builder().supertype(c2).build());
		typeInfoMap.put(c2, TypeInfo.builder().supertype(c1).build());

		assertThrows(IllegalArgumentException.class, () -> new TypeAnalyzer(typeInfoMap));
	}
}
