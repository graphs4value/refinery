/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.SymbolicParameter;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.Variable;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tools.refinery.logic.tests.QueryMatchers.structurallyEqualTo;

class StructurallyEqualToRawTest {
	private static final Constraint personView = new FakeKeyOnlyView("Person", 1);
	private static final Constraint friendView = new FakeKeyOnlyView("friend", 2);
	private static final NodeVariable p = Variable.of("p");
	private static final NodeVariable q = Variable.of("q");

	@Test
	void flatEqualsTest() {
		var actual = Dnf.builder("Actual").parameters(p).clause(personView.call(p)).build();

		assertThat(actual, structurallyEqualTo(
				List.of(new SymbolicParameter(q, ParameterDirection.OUT)),
				List.of(List.of(personView.call(q)))
		));
	}

	@Test
	void flatNotEqualsTest() {
		var actual = Dnf.builder("Actual").parameters(p).clause(friendView.call(p, q)).build();

		var assertion = structurallyEqualTo(
				List.of(new SymbolicParameter(q, ParameterDirection.OUT)),
				List.of(List.of(friendView.call(q, q)))
		);
		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}

	@Test
	void deepEqualsTest() {
		var actual = Dnf.builder("Actual").parameters(q).clause(
				Dnf.builder("Actual2").parameters(p).clause(personView.call(p)).build().call(q)
		).build();

		assertThat(actual, structurallyEqualTo(
				List.of(new SymbolicParameter(q, ParameterDirection.OUT)),
				List.of(
						List.of(
								Dnf.builder("Expected2").parameters(p).clause(personView.call(p)).build().call(q)
						)
				)
		));
	}

	@Test
	void deepNotEqualsTest() {
		var actual = Dnf.builder("Actual").parameter(q).clause(
				Dnf.builder("Actual2").parameters(p).clause(friendView.call(p, q)).build().call(q)
		).build();

		var assertion = structurallyEqualTo(
				List.of(new SymbolicParameter(q, ParameterDirection.OUT)),
				List.of(
						List.of(
								Dnf.builder("Expected2")
										.parameters(p)
										.clause(friendView.call(p, p))
										.build()
										.call(q)
						)
				)
		);
		var error = assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
		assertThat(error.getMessage(), allOf(containsString("Expected2"), containsString("Actual2")));
	}

	@Test
	void parameterListLengthMismatchTest() {
		var actual = Dnf.builder("Actual").parameters(p, q).clause(
				friendView.call(p, q)
		).build();

		var assertion = structurallyEqualTo(
				List.of(new SymbolicParameter(p, ParameterDirection.OUT)),
				List.of(List.of(friendView.call(p, p)))
		);

		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}

	@Test
	void parameterDirectionMismatchTest() {
		var actual = Dnf.builder("Actual").parameter(p, ParameterDirection.IN).clause(
				personView.call(p)
		).build();

		var assertion = structurallyEqualTo(
				List.of(new SymbolicParameter(p, ParameterDirection.OUT)),
				List.of(List.of(personView.call(p)))
		);

		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}

	@Test
	void clauseCountMismatchTest() {
		var actual = Dnf.builder("Actual").parameters(p, q).clause(
				friendView.call(p, q)
		).build();

		var assertion = structurallyEqualTo(
				List.of(
						new SymbolicParameter(p, ParameterDirection.OUT),
						new SymbolicParameter(q, ParameterDirection.OUT)
				),
				List.of(
						List.of(friendView.call(p, q)),
						List.of(friendView.call(q, p))
				)
		);

		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}

	@Test
	void literalCountMismatchTest() {
		var actual = Dnf.builder("Actual").parameters(p, q).clause(
				friendView.call(p, q)
		).build();

		var assertion = structurallyEqualTo(
				List.of(
						new SymbolicParameter(p, ParameterDirection.OUT),
						new SymbolicParameter(q, ParameterDirection.OUT)
				),
				List.of(
						List.of(friendView.call(p, q), friendView.call(q, p))
				)
		);

		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}
}
