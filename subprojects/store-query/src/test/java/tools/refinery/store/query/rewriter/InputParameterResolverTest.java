/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.rewriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.tests.QueryMatchers.structurallyEqualTo;

class InputParameterResolverTest {
	private final static Symbol<Boolean> person = Symbol.of("Person", 1);
	private final static Symbol<Boolean> friend = Symbol.of("friend", 2);
	private final static AnySymbolView personView = new KeyOnlyView<>(person);
	private final static AnySymbolView friendView = new KeyOnlyView<>(friend);

	private InputParameterResolver sut;

	@BeforeEach
	void beforeEach() {
		sut = new InputParameterResolver();
	}

	@Test
	void inlineSingleClauseTest() {
		var dnf = Dnf.of("SubQuery", builder -> {
			var x = builder.parameter("x", ParameterDirection.OUT);
			builder.clause(friendView.call(x, Variable.of()));
		});
		var query = Query.of("Actual", (builder, x) -> builder.clause(
				dnf.call(x),
				personView.call(x)
		));

		var actual = sut.rewrite(query);

		var expected = Query.of("Expected", (builder, x) -> builder.clause(
				friendView.call(x, Variable.of()),
				personView.call(x)
		));

		assertThat(actual.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void inlineSingleClauseWIthInputTest() {
		var dnf = Dnf.of("SubQuery", builder -> {
			var x = builder.parameter("x", ParameterDirection.IN);
			builder.clause(not(friendView.call(x, Variable.of())));
		});
		var query = Query.of("Actual", (builder, x) -> builder.clause(
				dnf.call(x),
				personView.call(x)
		));

		var actual = sut.rewrite(query);

		var expected = Query.of("Expected", (builder, x) -> builder.clause(
				personView.call(x),
				not(friendView.call(x, Variable.of()))
		));

		assertThat(actual.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void singleLiteralDemandSetTest() {
		var dnf = Dnf.of("SubQuery", builder -> {
			var x = builder.parameter("x", ParameterDirection.IN);
			builder.clause(not(friendView.call(x, Variable.of())));
			builder.clause(not(friendView.call(Variable.of(), x)));
		});
		var query = Query.of("Actual", (builder, x) -> builder.clause(
				dnf.call(x),
				personView.call(x)
		));

		var actual = sut.rewrite(query);

		var expectedSubQuery = Dnf.of("ExpectedSubQuery", builder -> {
			var x = builder.parameter("x", ParameterDirection.OUT);
			builder.clause(
					personView.call(x),
					not(friendView.call(x, Variable.of()))
			);
			builder.clause(
					personView.call(x),
					not(friendView.call(Variable.of(), x))
			);
		});
		var expected = Query.of("Expected", (builder, x) -> builder.clause(
				personView.call(x),
				expectedSubQuery.call(x)
		));

		assertThat(actual.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void multipleLiteralDemandSetTest() {
		var dnf = Dnf.of("SubQuery", builder -> {
			var x = builder.parameter("x", ParameterDirection.IN);
			var y = builder.parameter("y", ParameterDirection.IN);
			builder.clause(not(friendView.call(x, y)));
			builder.clause(not(friendView.call(y, x)));
		});
		var query = Query.of("Actual", (builder, p1) -> builder.clause(p2 -> List.of(
				not(dnf.call(p1, p2)),
				personView.call(p1),
				personView.call(p2)
		)));

		var actual = sut.rewrite(query);

		var context = Dnf.of("Context", builder -> {
			var x = builder.parameter("x", ParameterDirection.OUT);
			var y = builder.parameter("y", ParameterDirection.OUT);
			builder.clause(
					personView.call(x),
					personView.call(y)
			);
		});
		var expectedSubQuery = Dnf.of("ExpectedSubQuery", builder -> {
			var x = builder.parameter("x", ParameterDirection.OUT);
			var y = builder.parameter("x", ParameterDirection.OUT);
			builder.clause(
					context.call(x, y),
					not(friendView.call(x, y))
			);
			builder.clause(
					context.call(x, y),
					not(friendView.call(y, x))
			);
		});
		var expected = Query.of("Expected", (builder, p1) -> builder.clause(p2 -> List.of(
				context.call(p1, p2),
				not(expectedSubQuery.call(p1, p2))
		)));

		assertThat(actual.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void multipleParameterDemandSetTest() {
		var dnf = Dnf.of("SubQuery", builder -> {
			var x = builder.parameter("x", ParameterDirection.IN);
			var y = builder.parameter("y", ParameterDirection.IN);
			builder.clause(not(friendView.call(x, y)));
			builder.clause(not(friendView.call(y, x)));
		});
		var query = Query.of("Actual", (builder, p1) -> builder.clause(
				not(dnf.call(p1, p1)),
				personView.call(p1)
		));

		var actual = sut.rewrite(query);

		var expectedSubQuery = Dnf.of("ExpectedSubQuery", builder -> {
			var x = builder.parameter("x", ParameterDirection.OUT);
			var y = builder.parameter("y", ParameterDirection.OUT);
			builder.clause(
					y.isEquivalent(x),
					personView.call(x),
					not(friendView.call(x, x))
			);
		});
		var expected = Query.of("Expected", (builder, p1) -> builder.clause(
				personView.call(p1),
				not(expectedSubQuery.call(p1, p1))
		));

		assertThat(actual.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void eliminateDoubleNegationTest() {
		var dnf = Dnf.of("SubQuery", builder -> {
			var x = builder.parameter("x", ParameterDirection.IN);
			builder.clause(not(friendView.call(x, Variable.of())));
		});
		var query = Query.of("Actual", (builder, p1) -> builder.clause(
				personView.call(p1),
				not(dnf.call(p1))
		));

		var actual = sut.rewrite(query);

		var expected = Query.of("Actual", (builder, p1) -> builder.clause(
				personView.call(p1),
				friendView.call(p1, Variable.of())
		));

		assertThat(actual.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void identityWhenNoWorkToDoTest() {
		var dnf = Dnf.of("SubQuery", builder -> {
			var x = builder.parameter("x", ParameterDirection.OUT);
			builder.clause(
					personView.call(x),
					not(friendView.call(x, Variable.of()))
			);
		});
		var query = Query.of("Actual", (builder, p1) -> builder.clause(
				personView.call(p1),
				not(dnf.call(p1))
		));

		var actual = sut.rewrite(query);

		assertThat(actual, is(query));
	}
}
