/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TypeHierarchyPartialModelTest {
	private final PartialRelation person = new PartialRelation("Person", 1);
	private final PartialRelation member = new PartialRelation("Member", 1);
	private final PartialRelation student = new PartialRelation("Student", 1);
	private final PartialRelation teacher = new PartialRelation("Teacher", 1);
	private final PartialRelation pet = new PartialRelation("Pet", 1);

	private Model model;

	@BeforeEach
	void beforeEach() {
		var typeHierarchy = TypeHierarchy.builder()
				.type(person, true)
				.type(member, true, person)
				.type(student, member)
				.type(teacher, member)
				.type(pet)
				.build();

		var store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new TypeHierarchyTranslator(typeHierarchy))
				.build();

		var seed = ModelSeed.builder(4)
				.seed(person, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(3), TruthValue.FALSE))
				.seed(member, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(1), TruthValue.TRUE)
						.put(Tuple.of(2), TruthValue.TRUE))
				.seed(student, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE)
						.put(Tuple.of(2), TruthValue.FALSE))
				.seed(teacher, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(pet, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.build();
		model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed);
	}

	@Test
	void initialModelTest() {
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);

		var personInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, person);
		assertThat(personInterpretation.get(Tuple.of(0)), is(TruthValue.TRUE));
		assertThat(personInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(personInterpretation.get(Tuple.of(2)), is(TruthValue.TRUE));
		assertThat(personInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var memberInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, member);
		assertThat(memberInterpretation.get(Tuple.of(0)), is(TruthValue.TRUE));
		assertThat(memberInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(memberInterpretation.get(Tuple.of(2)), is(TruthValue.TRUE));
		assertThat(memberInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var studentInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, student);
		assertThat(studentInterpretation.get(Tuple.of(0)), is(TruthValue.TRUE));
		assertThat(studentInterpretation.get(Tuple.of(1)), is(TruthValue.UNKNOWN));
		assertThat(studentInterpretation.get(Tuple.of(2)), is(TruthValue.FALSE));
		assertThat(studentInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var teacherInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, teacher);
		assertThat(teacherInterpretation.get(Tuple.of(0)), is(TruthValue.FALSE));
		assertThat(teacherInterpretation.get(Tuple.of(1)), is(TruthValue.UNKNOWN));
		assertThat(teacherInterpretation.get(Tuple.of(2)), is(TruthValue.TRUE));
		assertThat(teacherInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var petInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, pet);
		assertThat(petInterpretation.get(Tuple.of(0)), is(TruthValue.FALSE));
		assertThat(petInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
		assertThat(petInterpretation.get(Tuple.of(2)), is(TruthValue.FALSE));
		assertThat(petInterpretation.get(Tuple.of(3)), is(TruthValue.UNKNOWN));
	}

	@Test
	void initialModelCandidateTest() {
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);

		var personCandidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, person);
		assertThat(personCandidateInterpretation.get(Tuple.of(0)), is(TruthValue.TRUE));
		assertThat(personCandidateInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(personCandidateInterpretation.get(Tuple.of(2)), is(TruthValue.TRUE));
		assertThat(personCandidateInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var memberCandidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, member);
		assertThat(memberCandidateInterpretation.get(Tuple.of(0)), is(TruthValue.TRUE));
		assertThat(memberCandidateInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(memberCandidateInterpretation.get(Tuple.of(2)), is(TruthValue.TRUE));
		assertThat(memberCandidateInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var studentCandidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, student);
		assertThat(studentCandidateInterpretation.get(Tuple.of(0)), is(TruthValue.TRUE));
		assertThat(studentCandidateInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(studentCandidateInterpretation.get(Tuple.of(2)), is(TruthValue.FALSE));
		assertThat(studentCandidateInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var teacherCandidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, teacher);
		assertThat(teacherCandidateInterpretation.get(Tuple.of(0)), is(TruthValue.FALSE));
		assertThat(teacherCandidateInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
		assertThat(teacherCandidateInterpretation.get(Tuple.of(2)), is(TruthValue.TRUE));
		assertThat(teacherCandidateInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var petCandidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, pet);
		assertThat(petCandidateInterpretation.get(Tuple.of(0)), is(TruthValue.FALSE));
		assertThat(petCandidateInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
		assertThat(petCandidateInterpretation.get(Tuple.of(2)), is(TruthValue.FALSE));
		assertThat(petCandidateInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));
	}

	@Test
	void refinedModelTest() {
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var studentRefiner = reasoningAdapter.getRefiner(student);
		studentRefiner.merge(Tuple.of(1), TruthValue.FALSE);
		studentRefiner.merge(Tuple.of(3), TruthValue.TRUE);
		model.getAdapter(ModelQueryAdapter.class).flushChanges();

		var personInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, person);
		assertThat(personInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(personInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));

		var personCandidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, person);
		assertThat(personCandidateInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(personCandidateInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var memberInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, member);
		assertThat(memberInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(memberInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));

		var memberCandidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, member);
		assertThat(memberCandidateInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(memberCandidateInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var studentInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, student);
		assertThat(studentInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
		assertThat(studentInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));

		var studentCandidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, student);
		assertThat(studentCandidateInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
		assertThat(studentCandidateInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var teacherInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, teacher);
		assertThat(teacherInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(teacherInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var teacherCandidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, teacher);
		assertThat(teacherCandidateInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(teacherCandidateInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var petInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, pet);
		assertThat(petInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
		assertThat(petInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));

		var petCandidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, pet);
		assertThat(petCandidateInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
		assertThat(petCandidateInterpretation.get(Tuple.of(3)), is(TruthValue.FALSE));
	}
}
