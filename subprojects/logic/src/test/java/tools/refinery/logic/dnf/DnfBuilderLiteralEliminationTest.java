/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.literal.BooleanLiteral;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.bool.BoolTerms;
import tools.refinery.logic.tests.FakeKeyOnlyView;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.tests.QueryMatchers.structurallyEqualTo;

class DnfBuilderLiteralEliminationTest {
	private final Constraint friendView = new FakeKeyOnlyView("friend", 2);
	private final NodeVariable p = Variable.of("p");
	private final NodeVariable q = Variable.of("q");
	private final Dnf trueDnf = Dnf.builder().parameter(p, ParameterDirection.IN).clause().build();
	private final Dnf falseDnf = Dnf.builder().parameter(p).build();

	@Test
	void eliminateTrueTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(BooleanLiteral.TRUE, friendView.call(p, q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateTrueAssumptionTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(check(BoolTerms.constant(true)), friendView.call(p, q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateFalseTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(friendView.call(q, p), BooleanLiteral.FALSE)
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@ParameterizedTest
	@CsvSource(value = {
			"false",
			"null"
	}, nullValues = "null")
	void eliminateFalseAssumptionTest(Boolean value) {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(friendView.call(q, p), check(BoolTerms.constant(value)))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysTrueTest() {
		var actual = Dnf.builder()
				.parameters(List.of(p, q), ParameterDirection.IN)
				.clause(friendView.call(p, q))
				.clause(BooleanLiteral.TRUE)
				.build();
		var expected = Dnf.builder().parameters(List.of(p, q), ParameterDirection.IN).clause().build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysFalseTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q), BooleanLiteral.FALSE)
				.build();
		var expected = Dnf.builder().parameters(p, q).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateTrueDnfTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(trueDnf.call(q), friendView.call(p, q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateFalseDnfTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(friendView.call(q, p), falseDnf.call(q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysTrueDnfTest() {
		var actual = Dnf.builder()
				.parameters(List.of(p, q), ParameterDirection.IN)
				.clause(friendView.call(p, q))
				.clause(trueDnf.call(q))
				.build();
		var expected = Dnf.builder().parameters(List.of(p, q), ParameterDirection.IN).clause().build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysFalseDnfTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q), falseDnf.call(q))
				.build();
		var expected = Dnf.builder().parameters(p, q).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateNotFalseDnfTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(not(falseDnf.call(q)), friendView.call(p, q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateNotTrueDnfTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(friendView.call(q, p), not(trueDnf.call(q)))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysNotFalseDnfTest() {
		var actual = Dnf.builder()
				.parameters(List.of(p, q), ParameterDirection.IN)
				.clause(friendView.call(p, q))
				.clause(not(falseDnf.call(q)))
				.build();
		var expected = Dnf.builder().parameters(List.of(p, q), ParameterDirection.IN).clause().build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysNotTrueDnfTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q), not(trueDnf.call(q)))
				.build();
		var expected = Dnf.builder().parameters(p, q).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void removeDuplicateTest() {
		var actual = Dnf.of(builder -> builder.clause((p, q) -> List.of(
				friendView.call(p, q),
				friendView.call(p, q)
		)));
		var expected = Dnf.of(builder -> builder.clause((p, q) -> List.of(friendView.call(p, q))));

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void removeContradictoryTest() {
		var actual = Dnf.of(builder -> builder.clause((p, q) -> List.of(
				friendView.call(p, q),
				not(friendView.call(p, q))
		)));
		var expected = Dnf.builder().build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void removeContradictoryUniversalTest() {
		var actual = Dnf.of(builder -> builder.clause((p, q) -> List.of(
				friendView.call(q, q),
				friendView.call(p, q),
				not(friendView.call(p, Variable.of()))
		)));
		var expected = Dnf.builder().build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void removeContradictoryExistentialUniversalTest() {
		var actual = Dnf.of(builder -> builder.clause((p) -> List.of(
				friendView.call(p, Variable.of()),
				not(friendView.call(p, Variable.of()))
		)));
		var expected = Dnf.builder().build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void removeContradictoryUniversalParameterTest() {
		var actual = Dnf.of(builder -> {
			var p = builder.parameter("p");
			builder.clause((q) -> List.of(
					friendView.call(q, q),
					friendView.call(p, q),
					not(friendView.call(p, Variable.of()))
			));
		});
		var expected = Dnf.builder().parameter(p).build();

		assertThat(actual, structurallyEqualTo(expected));
	}
}
