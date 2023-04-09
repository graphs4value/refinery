/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.representation.Symbol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.store.query.literal.Literals.not;

class DnfToDefinitionStringTest {
	@Test
	void noClausesTest() {
		var p = Variable.of("p");
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
		var p = Variable.of("p");
		var dnf = Dnf.builder("Example").parameter(p).clause().build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    <empty>.
				"""));
	}

	@Test
	void relationViewPositiveTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var dnf = Dnf.builder("Example").parameter(p).clause(friendView.call(p, q)).build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    @RelationView("key") friend(p, q).
				"""));
	}

	@Test
	void relationViewNegativeTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var dnf = Dnf.builder("Example").parameter(p).clause(not(friendView.call(p, q))).build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    !(@RelationView("key") friend(p, q)).
				"""));
	}

	@Test
	void relationViewTransitiveTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var dnf = Dnf.builder("Example").parameter(p).clause(friendView.callTransitive(p, q)).build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p) <->
				    @RelationView("key") friend+(p, q).
				"""));
	}

	@Test
	void multipleParametersTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var dnf = Dnf.builder("Example").parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(dnf.toDefinitionString(), is("""
				pred Example(p, q) <->
				    @RelationView("key") friend(p, q).
				"""));
	}

	@Test
	void multipleLiteralsTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var person = new Symbol<>("person", 1, Boolean.class, false);
		var personView = new KeyOnlyRelationView<>(person);
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
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
		var p = Variable.of("p");
		var q = Variable.of("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
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
		var p = Variable.of("p");
		var q = Variable.of("q");
		var r = Variable.of("r");
		var s = Variable.of("s");
		var person = new Symbol<>("person", 1, Boolean.class, false);
		var personView = new KeyOnlyRelationView<>(person);
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
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
