/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.TruthValue;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

class TypeAnalysisExampleHierarchyTest {
	private final PartialRelation a1 = new PartialRelation("A1", 1);
	private final PartialRelation a2 = new PartialRelation("A2", 1);
	private final PartialRelation a3 = new PartialRelation("A3", 1);
	private final PartialRelation a4 = new PartialRelation("A4", 1);
	private final PartialRelation a5 = new PartialRelation("A5", 1);
	private final PartialRelation c1 = new PartialRelation("C1", 1);
	private final PartialRelation c2 = new PartialRelation("C2", 1);
	private final PartialRelation c3 = new PartialRelation("C3", 1);
	private final PartialRelation c4 = new PartialRelation("C4", 1);

	private TypeHierarchy sut;
	private TypeHierarchyTester tester;

	@BeforeEach
	void beforeEach() {
		sut = TypeHierarchy.builder()
				.type(a1, true)
				.type(a2, true)
				.type(a3, true)
				.type(a4, true)
				.type(a5, true)
				.type(c1, a1, a4)
				.type(c2, a1, a2, a3, a4)
				.type(c3, a3)
				.type(c4, a4)
				.build();
		tester = new TypeHierarchyTester(sut);
	}

	@Test
	void analysisResultsTest() {
		assertAll(
				() -> tester.assertAbstractType(a1, c1, c2),
				() -> tester.assertEliminatedType(a2, c2),
				() -> tester.assertAbstractType(a3, c2, c3),
				() -> tester.assertAbstractType(a4, c1, c2, c4),
				() -> tester.assertVacuousType(a5),
				() -> tester.assertConcreteType(c1),
				() -> tester.assertConcreteType(c2),
				() -> tester.assertConcreteType(c3),
				() -> tester.assertConcreteType(c4)
		);
	}

	@Test
	void inferredTypesTest() {
		assertAll(
				() -> assertThat(sut.getUnknownType(), Matchers.is(new InferredType(Set.of(), Set.of(c1, c2, c3, c4), null))),
				() -> assertThat(tester.getInferredType(a1), Matchers.is(new InferredType(Set.of(a1, a4), Set.of(c1, c2), c1))),
				() -> assertThat(tester.getInferredType(a3), Matchers.is(new InferredType(Set.of(a3), Set.of(c2, c3), c2))),
				() -> assertThat(tester.getInferredType(a4), Matchers.is(new InferredType(Set.of(a4), Set.of(c1, c2, c4), c1))),
				() -> assertThat(tester.getInferredType(a5), Matchers.is(new InferredType(Set.of(a5), Set.of(), null))),
				() -> assertThat(tester.getInferredType(c1), Matchers.is(new InferredType(Set.of(a1, a4, c1), Set.of(c1), c1))),
				() -> assertThat(tester.getInferredType(c2),
						Matchers.is(new InferredType(Set.of(a1, a3, a4, c2), Set.of(c2), c2))),
				() -> assertThat(tester.getInferredType(c3), Matchers.is(new InferredType(Set.of(a3, c3), Set.of(c3), c3))),
				() -> assertThat(tester.getInferredType(c4), Matchers.is(new InferredType(Set.of(a4, c4), Set.of(c4), c4)))
		);
	}

	@Test
	void consistentMustTest() {
		var a1Result = tester.getPreservedType(a1);
		var a3Result = tester.getPreservedType(a3);
		var expected = new InferredType(Set.of(a1, a3, a4, c2), Set.of(c2), c2);
		assertAll(
				() -> assertThat(a1Result.merge(a3Result.asInferredType(), TruthValue.TRUE), Matchers.is(expected)),
				() -> assertThat(a3Result.merge(a1Result.asInferredType(), TruthValue.TRUE), Matchers.is(expected)),
				() -> assertThat(a1Result.merge(sut.getUnknownType(), TruthValue.TRUE), is(a1Result.asInferredType())),
				() -> assertThat(a3Result.merge(sut.getUnknownType(), TruthValue.TRUE), is(a3Result.asInferredType())),
				() -> assertThat(a1Result.merge(a1Result.asInferredType(), TruthValue.TRUE),
						is(a1Result.asInferredType()))
		);
	}

	@Test
	void consistentMayNotTest() {
		var a1Result = tester.getPreservedType(a1);
		var a3Result = tester.getPreservedType(a3);
		var a4Result = tester.getPreservedType(a4);
		assertAll(
				() -> assertThat(a1Result.merge(a3Result.asInferredType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(a3, c3), Set.of(c3), c3))),
				() -> assertThat(a3Result.merge(a1Result.asInferredType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(a1, a4, c1), Set.of(c1), c1))),
				() -> assertThat(a4Result.merge(a3Result.asInferredType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(a3, c3), Set.of(c3), c3))),
				() -> assertThat(a3Result.merge(a4Result.asInferredType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(a4), Set.of(c1, c4), c1))),
				() -> assertThat(a1Result.merge(sut.getUnknownType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(), Set.of(c3, c4), null))),
				() -> assertThat(a3Result.merge(sut.getUnknownType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(), Set.of(c1, c4), null))),
				() -> assertThat(a4Result.merge(sut.getUnknownType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(), Set.of(c3), null)))
		);
	}

	@Test
	void consistentErrorTest() {
		var c1Result = tester.getPreservedType(c1);
		var a4Result = tester.getPreservedType(a4);
		var expected = new InferredType(Set.of(c1, a1, a4), Set.of(), null);
		assertAll(
				() -> assertThat(c1Result.merge(a4Result.asInferredType(), TruthValue.ERROR), Matchers.is(expected)),
				() -> assertThat(a4Result.merge(c1Result.asInferredType(), TruthValue.ERROR), Matchers.is(expected))
		);
	}

	@Test
	void consistentUnknownTest() {
		var c1Result = tester.getPreservedType(c1);
		var a4Result = tester.getPreservedType(a4);
		assertAll(
				() -> assertThat(c1Result.merge(a4Result.asInferredType(), TruthValue.UNKNOWN),
						is(a4Result.asInferredType())),
				() -> assertThat(a4Result.merge(c1Result.asInferredType(), TruthValue.UNKNOWN),
						is(c1Result.asInferredType()))
		);
	}

	@Test
	void inconsistentMustTest() {
		var a1Result = tester.getPreservedType(a1);
		var c3Result = tester.getPreservedType(c3);
		assertAll(
				() -> assertThat(a1Result.merge(c3Result.asInferredType(), TruthValue.TRUE),
						Matchers.is(new InferredType(Set.of(a1, a3, c3), Set.of(), null))),
				() -> assertThat(c3Result.merge(a1Result.asInferredType(), TruthValue.TRUE),
						Matchers.is(new InferredType(Set.of(a1, a3, a4, c3), Set.of(), null)))
		);
	}

	@Test
	void inconsistentMayNotTest() {
		var a1Result = tester.getPreservedType(a1);
		var a4Result = tester.getPreservedType(a4);
		var c1Result = tester.getPreservedType(c1);
		assertAll(
				() -> assertThat(a4Result.merge(a1Result.asInferredType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(a1, a4), Set.of(), null))),
				() -> assertThat(a1Result.merge(c1Result.asInferredType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(a1, a4, c1), Set.of(), null))),
				() -> assertThat(a4Result.merge(c1Result.asInferredType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(a1, a4, c1), Set.of(), null))),
				() -> assertThat(a1Result.merge(a1Result.asInferredType(), TruthValue.FALSE),
						Matchers.is(new InferredType(Set.of(a1, a4), Set.of(), null)))
		);
	}

	@Test
	void vacuousMustTest() {
		var c1Result = tester.getPreservedType(c1);
		var a5Result = tester.getPreservedType(a5);
		assertAll(
				() -> assertThat(c1Result.merge(a5Result.asInferredType(), TruthValue.TRUE),
						Matchers.is(new InferredType(Set.of(a1, a4, a5, c1), Set.of(), null))),
				() -> assertThat(a5Result.merge(c1Result.asInferredType(), TruthValue.TRUE),
						Matchers.is(new InferredType(Set.of(a1, a4, a5, c1), Set.of(), null)))
		);
	}

	@Test
	void vacuousMayNotTest() {
		var c1Result = tester.getPreservedType(c1);
		var a5Result = tester.getPreservedType(a5);
		assertAll(
				() -> assertThat(c1Result.merge(a5Result.asInferredType(), TruthValue.FALSE),
						is(a5Result.asInferredType())),
				() -> assertThat(a5Result.merge(c1Result.asInferredType(), TruthValue.FALSE),
						is(c1Result.asInferredType()))
		);
	}

	@Test
	void vacuousErrorTest() {
		var c1Result = tester.getPreservedType(c1);
		var a5Result = tester.getPreservedType(a5);
		assertAll(
				() -> assertThat(c1Result.merge(a5Result.asInferredType(), TruthValue.ERROR),
						Matchers.is(new InferredType(Set.of(a1, a4, a5, c1), Set.of(), null))),
				() -> assertThat(a5Result.merge(c1Result.asInferredType(), TruthValue.ERROR),
						Matchers.is(new InferredType(Set.of(a1, a4, a5, c1), Set.of(), null))),
				() -> assertThat(a5Result.merge(a5Result.asInferredType(), TruthValue.ERROR),
						is(a5Result.asInferredType()))
		);
	}
}
