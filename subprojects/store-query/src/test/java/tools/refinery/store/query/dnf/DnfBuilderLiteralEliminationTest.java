/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.refinery.store.query.literal.BooleanLiteral;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.bool.BoolTerms;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.query.view.SymbolView;
import tools.refinery.store.representation.Symbol;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static tools.refinery.store.query.literal.Literals.assume;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.tests.QueryMatchers.structurallyEqualTo;

class DnfBuilderLiteralEliminationTest {
	private final Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class, false);
	private final SymbolView<Boolean> friendView = new KeyOnlyView<>(friend);
	private final NodeVariable p = Variable.of("p");
	private final NodeVariable q = Variable.of("q");
	private final Dnf trueDnf = Dnf.builder().parameter(p).clause().build();
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
				.clause(assume(BoolTerms.constant(true)), friendView.call(p, q))
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
				.clause(friendView.call(q, p), assume(BoolTerms.constant(value)))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysTrueTest() {
		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(BooleanLiteral.TRUE)
				.build();
		var expected = Dnf.builder().parameters(p, q).clause().build();

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
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(trueDnf.call(q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause().build();

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
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(not(falseDnf.call(q)))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause().build();

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
}
