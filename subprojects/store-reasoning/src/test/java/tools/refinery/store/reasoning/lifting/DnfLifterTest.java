/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.lifting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.MustView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.*;
import static tools.refinery.store.query.tests.QueryMatchers.structurallyEqualTo;

class DnfLifterTest {
	private static final Symbol<TruthValue> friendSymbol = Symbol.of("friend", 2, TruthValue.class,
			TruthValue.UNKNOWN);
	private static final AnySymbolView friendMustView = new MustView(friendSymbol);
	private static final Symbol<Integer> age = Symbol.of("age", 1, Integer.class);
	private static final FunctionView<Integer> ageView = new FunctionView<>(age);
	private static final PartialRelation person = PartialSymbol.of("Person", 1);
	private static final PartialRelation friend = PartialSymbol.of("friend", 2);

	private DnfLifter sut;

	@BeforeEach
	void beforeEach() {
		sut = new DnfLifter();
	}

	@Test
	void liftPartialRelationCallTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause((v1) -> List.of(
			friend.call(p1, v1)
		))).getDnf();
		var actual = sut.lift(Modality.MUST, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", (builder, p1) -> builder.clause((v1) -> List.of(
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, friend).call(p1, v1),
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(v1)
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftPartialDnfCallTest() {
		var called = Query.of("Called", (builder, p1, p2) -> builder.clause(
				friend.call(p1, p2),
				friend.call(p2, p1)
		));
		var input = Query.of("Actual", (builder, p1) -> builder.clause((v1) -> List.of(
				called.call(p1, v1)
		))).getDnf();
		var actual = sut.lift(Modality.MUST, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", (builder, p1) -> builder.clause((v1) -> List.of(
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, called.getDnf()).call(p1, v1),
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(v1)
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftSymbolViewCallTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause((v1) -> List.of(
				friendMustView.call(p1, v1)
		))).getDnf();
		var actual = sut.lift(Modality.MUST, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", (builder, p1) -> builder.clause((v1) -> List.of(
				friendMustView.call(p1, v1),
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(v1)
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftPartialRelationNegativeCallTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause((v1) -> List.of(
				not(friend.call(p1, v1)),
				friend.call(v1, p1)
		))).getDnf();
		var actual = sut.lift(Modality.MUST, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", (builder, p1) -> builder.clause((v1) -> List.of(
				not(ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, friend).call(p1, v1)),
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, friend).call(v1, p1),
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(v1)
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftPartialRelationQuantifiedNegativeCallTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause((v1) -> List.of(
				person.call(p1),
				not(friend.call(p1, v1))
		))).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var helper = Query.of("Helper", (builder, p1, p2) -> builder.clause(
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, friend).call(p1, p2),
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(p2)
		));
		var expected = Query.of("Expected", (builder, p1) -> builder.clause((v1) -> List.of(
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, person).call(p1),
				not(helper.call(p1, v1))
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftSymbolViewQuantifiedNegativeCallTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause((v1) -> List.of(
				person.call(p1),
				not(friendMustView.call(p1, v1))
		))).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var helper = Query.of("Helper", (builder, p1, p2) -> builder.clause(
				friendMustView.call(p1, p2),
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(p2)
		));
		var expected = Query.of("Expected", (builder, p1) -> builder.clause((v1) -> List.of(
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, person).call(p1),
				not(helper.call(p1, v1))
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftPartialRelationQuantifiedNegativeDiagonalCallTest() {
		var input = Query.of("Actual", (builder) -> builder.clause((v1) -> List.of(
				not(friend.call(v1, v1))
		))).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var helper = Query.of("Helper", (builder, p1) -> builder.clause(
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, friend).call(p1, p1),
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(p1)
		));
		var expected = Query.of("Expected", (builder) -> builder.clause((v1) -> List.of(
				not(helper.call(v1))
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftPartialDnfQuantifiedNegativeInputCallTest() {
		var called = Dnf.of("Called", builder -> {
			var p1 = builder.parameter("p1", ParameterDirection.IN);
			var p2 = builder.parameter("p2", ParameterDirection.OUT);
			builder.clause(
					friend.call(p1, p2),
					friend.call(p2, p1)
			);
		});
		var input = Query.of("Actual", (builder, p1) -> builder.clause((v1) -> List.of(
				person.call(p1),
				not(called.call(p1, v1))
		))).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var helper = Dnf.of("Helper", builder -> {
			var p1 = builder.parameter("p1", ParameterDirection.IN);
			var p2 = builder.parameter("p2", ParameterDirection.OUT);
			builder.clause(
					ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, called).call(p1, p2),
					ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(p2)
			);
		});
		var expected = Query.of("Expected", (builder, p1) -> builder.clause((v1) -> List.of(
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, person).call(p1),
				not(helper.call(p1, v1))
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftPartialRelationTransitiveCallTest() {
		var input = Query.of("Actual", (builder, p1, p2)-> builder.clause(
				friend.callTransitive(p1, p2),
				not(person.call(p2))
		)).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var helper = Query.of("Helper", (builder, p1, p2) -> builder.clause(
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, friend).call(p1, p2),
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(p2)
		));
		var helper2 = Query.of("Helper2", (builder, p1, p2) -> {
			builder.clause(
					ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, friend).call(p1, p2)
			);
			builder.clause((v1) -> List.of(
					helper.callTransitive(p1, v1),
					ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, friend).call(v1, p2)
			));
		});
		var expected = Query.of("Expected", (builder, p1, p2) -> builder.clause(
				helper2.call(p1, p2),
				not(ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, person).call(p2))
		)).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftPartialSymbolTransitiveCallTest() {
		var input = Query.of("Actual", (builder, p1, p2)-> builder.clause(
				friendMustView.callTransitive(p1, p2),
				not(person.call(p2))
		)).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var endExistsHelper = Query.of("EndExistsHelper", (builder, p1, p2) -> builder.clause(
				friendMustView.call(p1, p2),
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(p2)
		));
		var transitiveHelper = Query.of("TransitiveHelper", (builder, p1, p2) -> {
			builder.clause(
					friendMustView.call(p1, p2)
			);
			builder.clause((v1) -> List.of(
					endExistsHelper.callTransitive(p1, v1),
					friendMustView.call(v1, p2)
			));
		});
		var expected = Query.of("Expected", (builder, p1, p2) -> builder.clause(
				transitiveHelper.call(p1, p2),
				not(ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, person).call(p2))
		)).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftPartialRelationTransitiveCallExistsTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause((v1) -> List.of(
				friend.callTransitive(p1, v1),
				not(person.call(v1))
		))).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var helper = Query.of("Helper", (builder, p1, p2) -> builder.clause(
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, friend).call(p1, p2),
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(p2)
		));
		var expected = Query.of("Expected", (builder, p1) -> builder.clause((v1) -> List.of(
				helper.callTransitive(p1, v1),
				not(ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, person).call(v1))
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftMultipleTransitiveCallExistsTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause((v1) -> List.of(
				friend.callTransitive(p1, v1),
				friendMustView.callTransitive(p1, v1),
				not(person.call(v1))
		))).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var helper = Query.of("Helper", (builder, p1, p2) -> builder.clause(
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, friend).call(p1, p2),
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(p2)
		));
		var helper2 = Query.of("Helper2", (builder, p1, p2) -> builder.clause(
				friendMustView.call(p1, p2),
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(p2)
		));
		var expected = Query.of("Expected", (builder, p1) -> builder.clause((v1) -> List.of(
				helper.callTransitive(p1, v1),
				helper2.callTransitive(p1, v1),
				not(ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, person).call(v1))
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftEquivalentTest() {
		var input = Query.of("Actual", (builder, p1, p2) -> builder.clause(
				p1.isEquivalent(p2),
				person.call(p1)
		)).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", (builder, p1, p2) -> builder.clause(
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, person).call(p1),
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, ReasoningAdapter.EQUALS_SYMBOL).call(p2, p1)
		)).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftNotEquivalentTest() {
		var input = Query.of("Actual", (builder, p1, p2) -> builder.clause(
				not(p1.isEquivalent(p2)),
				friend.call(p1, p2)
		)).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", (builder, p1, p2) -> builder.clause(
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, friend).call(p1, p2),
				not(ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, ReasoningAdapter.EQUALS_SYMBOL).call(p1, p2))
		)).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftConstantTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause((v1) -> List.of(
				v1.isConstant(0),
				friend.call(v1, p1)
		))).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", (builder, p1) -> builder.clause((v1) -> List.of(
				v1.isConstant(0),
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, friend).call(v1, p1),
				ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, ReasoningAdapter.EXISTS_SYMBOL).call(v1)
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftAssignTest() {
		var input = Query.of("Actual", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, (d1) -> List.of(
						person.call(p1),
						ageView.call(p1, d1),
						output.assign(mul(constant(2), d1))
				))).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, (d1) -> List.of(
						ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL, person).call(p1),
						ageView.call(p1, d1),
						output.assign(mul(constant(2), d1))
				))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftCheckTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause(Integer.class, (d1) -> List.of(
				person.call(p1),
				ageView.call(p1, d1),
				check(greaterEq(d1, constant(21)))
		))).getDnf();
		var actual = sut.lift(Modality.MAY, Concreteness.CANDIDATE, input);

		var expected = Query.of("Expected", (builder, p1) -> builder.clause(Integer.class, (d1) -> List.of(
				ModalConstraint.of(Modality.MAY, Concreteness.CANDIDATE, person).call(p1),
				ageView.call(p1, d1),
				check(greaterEq(d1, constant(21)))
		))).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}
}
