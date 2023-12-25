/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MetamodelTest {
	private final PartialRelation person = new PartialRelation("Person", 1);
	private final PartialRelation student = new PartialRelation("Student", 1);
	private final PartialRelation teacher = new PartialRelation("Teacher", 1);
	private final PartialRelation university = new PartialRelation("University", 1);
	private final PartialRelation course = new PartialRelation("Course", 1);
	private final PartialRelation courses = new PartialRelation("courses", 2);
	private final PartialRelation location = new PartialRelation("location", 2);
	private final PartialRelation lecturer = new PartialRelation("lecturer", 2);
	private final PartialRelation invalidLecturerCount = new PartialRelation("invalidLecturerCount", 1);
	private final PartialRelation enrolledStudents = new PartialRelation("enrolledStudents", 2);
	private final PartialRelation invalidStudentCount = new PartialRelation("invalidStudentCount", 1);

	@Test
	void metamodelTest() {
		var metamodel = Metamodel.builder()
				.type(person, true)
				.type(student, person)
				.type(teacher, person)
				.type(university)
				.type(course)
				.reference(courses, builder -> builder
						.containment(true)
						.source(university)
						.target(course)
						.opposite(location))
				.reference(location, builder -> builder
						.source(course)
						.target(university)
						.opposite(courses))
				.reference(lecturer, builder -> builder
						.source(course)
						.multiplicity(CardinalityIntervals.ONE, invalidLecturerCount)
						.target(teacher))
				.reference(enrolledStudents, builder -> builder
						.source(course)
						.multiplicity(CardinalityIntervals.SOME, invalidStudentCount)
						.target(student))
				.build();

		var seed = ModelSeed.builder(5)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(1), CardinalityIntervals.SET)
						.put(Tuple.of(4), CardinalityIntervals.SET))
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINS_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(person, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(student, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(teacher, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(university, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE))
				.seed(course, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(2), TruthValue.TRUE))
				.seed(courses, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(location, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(1, 0), TruthValue.TRUE))
				.seed(lecturer, builder -> builder
						.reducedValue(TruthValue.FALSE)
						.put(Tuple.of(1, 3), TruthValue.TRUE))
				.seed(enrolledStudents, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.build();

		var model = createModel(metamodel, seed);
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);

		var coursesInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, courses);
		assertThat(coursesInterpretation.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
		assertThat(coursesInterpretation.get(Tuple.of(0, 2)), is(TruthValue.UNKNOWN));
		assertThat(coursesInterpretation.get(Tuple.of(0, 3)), is(TruthValue.FALSE));

		var invalidLecturerCountInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL,
				invalidLecturerCount);
		assertThat(invalidLecturerCountInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
		assertThat(invalidLecturerCountInterpretation.get(Tuple.of(2)), is(TruthValue.ERROR));

		var enrolledStudentsInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL,
				enrolledStudents);
		assertThat(enrolledStudentsInterpretation.get(Tuple.of(1, 3)), is(TruthValue.FALSE));
		assertThat(enrolledStudentsInterpretation.get(Tuple.of(1, 4)), is(TruthValue.UNKNOWN));
	}

	@Test
	void simpleContainmentTest() {
		var metamodel = Metamodel.builder()
				.type(university)
				.type(course)
				.reference(courses, builder -> builder
						.containment(true)
						.source(university)
						.target(course))
				.build();

		var seed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.SET)
						.put(Tuple.of(1), CardinalityIntervals.SET))
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINS_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(university, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE))
				.seed(course, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(1), TruthValue.TRUE))
				.seed(courses, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(2, 3), TruthValue.TRUE))
				.build();

		var model = createModel(metamodel, seed);
		var coursesInterpretation = model.getAdapter(ReasoningAdapter.class)
				.getPartialInterpretation(Concreteness.PARTIAL, courses);

		assertThat(coursesInterpretation.get(Tuple.of(0, 1)), is(TruthValue.UNKNOWN));
		assertThat(coursesInterpretation.get(Tuple.of(0, 3)), is(TruthValue.FALSE));
		assertThat(coursesInterpretation.get(Tuple.of(2, 1)), is(TruthValue.UNKNOWN));
		assertThat(coursesInterpretation.get(Tuple.of(2, 3)), is(TruthValue.TRUE));
	}

	private static Model createModel(Metamodel metamodel, ModelSeed seed) {
		var store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(new MetamodelTranslator(metamodel))
				.build();

		return store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed);
	}
}
