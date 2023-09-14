/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.store.query.literal.Literals.not;

class DnfToDefinitionStringTest {
	private static final Symbol<Boolean> person = Symbol.of("person", 1);
	private static final Symbol<Boolean> friend = Symbol.of("friend", 2);
	private static final AnySymbolView personView = new KeyOnlyView<>(person);
	private static final AnySymbolView friendView = new KeyOnlyView<>(friend);
	private static final NodeVariable p = Variable.of("p");
	private static final NodeVariable q = Variable.of("q");

	@Test
	void noClausesTest() {
		var dnf = Dnf.builder("Example").parameter(p).build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    <no clauses>.
				"""));
	}

	@Test
	void noParametersTest() {
		var dnf = Dnf.builder("Example").build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example() <->
				    <no clauses>.
				"""));
	}

	@Test
	void emptyClauseTest() {
		var dnf = Dnf.builder("Example").parameter(p, ParameterDirection.IN).clause().build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(in p) <->
				    <empty>.
				"""));
	}

	@Test
	void relationViewPositiveTest() {
		var dnf = Dnf.builder("Example").parameter(p).clause(friendView.call(p, q)).build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    @RelationView("key") friend(p, q).
				"""));
	}

	@Test
	void relationViewNegativeTest() {
		var dnf = Dnf.builder("Example")
				.parameter(p, ParameterDirection.IN)
				.clause(not(friendView.call(p, q)))
				.build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(in p) <->
				    !(@RelationView("key") friend(p, q)).
				"""));
	}

	@Test
	void relationViewTransitiveTest() {
		var dnf = Dnf.builder("Example").parameter(p).clause(friendView.callTransitive(p, q)).build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    @RelationView("key") friend+(p, q).
				"""));
	}

	@Test
	void multipleParametersTest() {
		var dnf = Dnf.builder("Example").parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p, q) <->
				    @RelationView("key") friend(p, q).
				"""));
	}

	@Test
	void multipleLiteralsTest() {
		var dnf = Dnf.builder("Example")
				.parameter(p)
				.clause(
						personView.call(p),
						personView.call(q),
						friendView.call(p, q)
				)
				.build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    @RelationView("key") person(p),
				    @RelationView("key") person(q),
				    @RelationView("key") friend(p, q).
				"""));
	}

	@Test
	void multipleClausesTest() {
		var dnf = Dnf.builder("Example")
				.parameter(p)
				.clause(friendView.call(p, q))
				.clause(friendView.call(q, p))
				.build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    @RelationView("key") friend(p, q)
				;
				    @RelationView("key") friend(q, p).
				"""));
	}

	@Test
	void dnfTest() {
		var r = Variable.of("r");
		var s = Variable.of("s");
		var called  = Dnf.builder("Called").parameters(r, s).clause(friendView.call(r, s)).build();
		var dnf = Dnf.builder("Example")
				.parameter(p)
				.clause(
						personView.call(p),
						personView.call(q),
						not(called.call(p, q))
				)
				.build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    @RelationView("key") person(p),
				    @RelationView("key") person(q),
				    !(@Dnf Called(p, q)).
				"""));
	}
}
