/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.tests.FakeKeyOnlyView;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.logic.literal.Literals.not;

class DnfToDefinitionStringTest {
	private static final Constraint personView = new FakeKeyOnlyView("person", 1);
	private static final Constraint friendView = new FakeKeyOnlyView("friend", 2);
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
				    friend(p, q).
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
				    !(friend(p, q)).
				"""));
	}

	@Test
	void relationViewTransitiveTest() {
		var dnf = Dnf.builder("Example").parameter(p).clause(friendView.callTransitive(p, q)).build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    friend+(p, q).
				"""));
	}

	@Test
	void multipleParametersTest() {
		var dnf = Dnf.builder("Example").parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p, q) <->
				    friend(p, q).
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
				    person(p),
				    person(q),
				    friend(p, q).
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
				    friend(p, q)
				;
				    friend(q, p).
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
				    person(p),
				    person(q),
				    !(@Dnf Called(p, q)).
				"""));
	}
}
