/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.tests.QueryMatchers.structurallyEqualTo;

class TopologicalSortTest {
	private static final Symbol<Boolean> friend = Symbol.of("friend", 2);
	private static final AnySymbolView friendView = new KeyOnlyView<>(friend);
	private static final Dnf example = Dnf.of("example", builder -> {
		var a = builder.parameter("a", ParameterDirection.IN);
		var b = builder.parameter("b", ParameterDirection.IN);
		var c = builder.parameter("c", ParameterDirection.OUT);
		var d = builder.parameter("d", ParameterDirection.OUT);
		builder.clause(
				friendView.call(a, b),
				friendView.call(b, c),
				friendView.call(c, d)
		);
	});
	private static final NodeVariable p = Variable.of("p");
	private static final NodeVariable q = Variable.of("q");
	private static final NodeVariable r = Variable.of("r");
	private static final NodeVariable s = Variable.of("s");
	private static final NodeVariable t = Variable.of("t");

	@Test
	void topologicalSortTest() {
		var actual = Dnf.builder("Actual")
				.parameter(p, ParameterDirection.IN)
				.parameter(q, ParameterDirection.OUT)
				.clause(
						not(friendView.call(p, q)),
						example.call(p, q, r, s),
						example.call(r, t, q, s),
						friendView.call(r, t)
				)
				.build();

		assertThat(actual, structurallyEqualTo(
				List.of(
						new SymbolicParameter(p, ParameterDirection.IN),
						new SymbolicParameter(q, ParameterDirection.OUT)
				),
				List.of(
						List.of(
								friendView.call(r, t),
								example.call(r, t, q, s),
								not(friendView.call(p, q)),
								example.call(p, q, r, s)
						)
				)
		));
	}

	@Test
	void missingInputTest() {
		var builder = Dnf.builder("Actual")
				.parameter(p, ParameterDirection.OUT)
				.parameter(q, ParameterDirection.OUT)
				.clause(
						not(friendView.call(p, q)),
						example.call(p, q, r, s),
						example.call(r, t, q, s),
						friendView.call(r, t)
				);
		assertThrows(InvalidQueryException.class, builder::build);
	}

	@Test
	void missingVariableTest() {
		var builder = Dnf.builder("Actual")
				.parameter(p, ParameterDirection.IN)
				.parameter(q, ParameterDirection.OUT)
				.clause(
						not(friendView.call(p, q)),
						example.call(p, q, r, s),
						example.call(r, t, q, s)
				);
		assertThrows(InvalidQueryException.class, builder::build);
	}

	@Test
	void circularDependencyTest() {
		var builder = Dnf.builder("Actual")
				.parameter(p, ParameterDirection.IN)
				.parameter(q, ParameterDirection.OUT)
				.clause(
						not(friendView.call(p, q)),
						example.call(p, q, r, s),
						example.call(r, t, q, s),
						example.call(p, q, r, t)
				);
		assertThrows(InvalidQueryException.class, builder::build);
	}
}
