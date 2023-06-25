/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class HashCodeTest {
	private static final Symbol<Boolean> person = Symbol.of("Person", 1);
	private static final Symbol<Boolean> friend = Symbol.of("friend", 2);
	private static final AnySymbolView personView = new KeyOnlyView<>(person);
	private static final AnySymbolView friendView = new KeyOnlyView<>(friend);
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
