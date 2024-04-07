/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.tests.FakeKeyOnlyView;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class HashCodeTest {
	private static final Constraint personView = new FakeKeyOnlyView("Person", 1);
	private static final Constraint friendView = new FakeKeyOnlyView("friend", 2);
	private static final NodeVariable p = Variable.of("p");
	private static final NodeVariable q = Variable.of("q");

	@Test
	void flatEqualsTest() {
		var expected = Dnf.builder("Expected").parameters(q).clause(personView.call(q)).build();
		var actual = Dnf.builder("Actual").parameters(p).clause(personView.call(p)).build();

		assertThat(actual.hashCodeWithSubstitution(), is(expected.hashCodeWithSubstitution()));
	}

	@Test
	void flatNotEqualsTest() {
		var expected = Dnf.builder("Expected").parameters(q).clause(friendView.call(q, q)).build();
		var actual = Dnf.builder("Actual").parameters(p).clause(friendView.call(p, q)).build();

		assertThat(actual.hashCodeWithSubstitution(), not(expected.hashCodeWithSubstitution()));
	}

	@Test
	void deepEqualsTest() {
		var expected2 = Dnf.builder("Expected2").parameters(p).clause(personView.call(p)).build();
		var expected = Dnf.builder("Expected").parameters(q).clause(
				expected2.call(q)
		).build();
		var actual = Dnf.builder("Actual").parameters(q).clause(
				expected2.call(q)
		).build();

		assertThat(actual.hashCodeWithSubstitution(), is(expected.hashCodeWithSubstitution()));
	}

	@Test
	void deepNotEqualsTest() {
		var expected = Dnf.builder("Expected").parameters(q).clause(
				Dnf.builder("Expected2").parameters(p).clause(personView.call(p)).build().call(q)
		).build();
		var actual = Dnf.builder("Actual").parameters(q).clause(
				Dnf.builder("Actual2").parameters(p).clause(personView.call(p)).build().call(q)
		).build();

		assertThat(actual.hashCodeWithSubstitution(), not(expected.hashCodeWithSubstitution()));
	}
}
