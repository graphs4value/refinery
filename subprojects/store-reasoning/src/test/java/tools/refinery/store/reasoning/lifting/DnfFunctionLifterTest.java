/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.lifting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.term.intinterval.IntIntervalTerms.constant;
import static tools.refinery.logic.term.intinterval.IntIntervalTerms.greaterEq;
import static tools.refinery.logic.term.truthvalue.TruthValueTerms.must;
import static tools.refinery.logic.tests.QueryMatchers.structurallyEqualTo;
import static tools.refinery.store.reasoning.literal.PartialLiterals.partialCheck;

class DnfFunctionLifterTest {
	private static final PartialRelation person = PartialSymbol.of("Person", 1);
	private static final PartialFunction<IntInterval, BigInteger> age = PartialSymbol.of("age", 1,
			IntIntervalDomain.INSTANCE);

	private DnfLifter sut;

	@BeforeEach
	void beforeEach() {
		sut = new DnfLifter();
	}

	@Test
	void liftPartialFunctionCallTest() {
		var input = Query.of("Actual", IntInterval.class, (builder, p1, output) -> builder.clause(
				person.call(p1),
				output.assign(age.call(p1))
		)).getDnf();
		var actual = sut.lift(Modality.MUST, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", IntInterval.class, (builder, p1, output) -> builder.clause(
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, person).call(p1),
				output.assign(age.call(Concreteness.PARTIAL, p1))
		)).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftPartialFunctionCallWithConcretenessTest() {
		var input = Query.of("Actual", IntInterval.class, (builder, p1, output) -> builder.clause(
				person.call(p1),
				output.assign(age.call(Concreteness.CANDIDATE, p1))
		)).getDnf();
		var actual = sut.lift(Modality.MUST, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", IntInterval.class, (builder, p1, output) -> builder.clause(
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, person).call(p1),
				output.assign(age.call(Concreteness.CANDIDATE, p1))
		)).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftPartialCheckTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause(
				person.call(p1),
				partialCheck(greaterEq(age.call(p1), constant(IntInterval.of(18))))
		)).getDnf();

		var actual = sut.lift(Modality.MUST, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", (builder, p1) -> builder.clause(
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, person).call(p1),
				check(must(greaterEq(age.call(Concreteness.PARTIAL, p1), constant(IntInterval.of(18)))))
		)).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void liftCheckTest() {
		var input = Query.of("Actual", (builder, p1) -> builder.clause(
				person.call(p1),
				check(must(greaterEq(age.call(p1), constant(IntInterval.of(18)))))
		)).getDnf();

		var actual = sut.lift(Modality.MUST, Concreteness.PARTIAL, input);

		var expected = Query.of("Expected", (builder, p1) -> builder.clause(
				ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL, person).call(p1),
				check(must(greaterEq(age.call(Concreteness.PARTIAL, p1), constant(IntInterval.of(18)))))
		)).getDnf();

		assertThat(actual, structurallyEqualTo(expected));
	}
}
