/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Parameter;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static tools.refinery.store.query.literal.Literals.not;

class CallLiteralTest {
	private static final NodeVariable p = Variable.of("p");
	private static final NodeVariable q = Variable.of("q");
	private static final NodeVariable r = Variable.of("r");
	private static final NodeVariable s = Variable.of("s");

	private static final Constraint fakeConstraint = new Constraint() {
		@Override
		public String name() {
			return getClass().getName();
		}

		@Override
		public List<Parameter> getParameters() {
			return List.of(
					new Parameter(null, ParameterDirection.IN),
					new Parameter(null, ParameterDirection.IN),
					new Parameter(null, ParameterDirection.OUT),
					new Parameter(null, ParameterDirection.OUT)
			);
		}
	};

	@Test
	void notRepeatedPositiveDirectionTest() {
		var literal = fakeConstraint.call(p, q, r, s);
		assertAll(
				() -> assertThat(literal.getOutputVariables(), containsInAnyOrder(r, s)),
				() -> assertThat(literal.getInputVariables(Set.of()), containsInAnyOrder(p, q)),
				() -> assertThat(literal.getInputVariables(Set.of(p, q, r)), containsInAnyOrder(p, q)),
				() -> assertThat(literal.getPrivateVariables(Set.of()), empty()),
				() -> assertThat(literal.getPrivateVariables(Set.of(p, q, r)), empty())
		);
	}

	@Test
	void notRepeatedNegativeDirectionTest() {
		var literal = not(fakeConstraint.call(p, q, r, s));
		assertAll(
				() -> assertThat(literal.getOutputVariables(), empty()),
				() -> assertThat(literal.getInputVariables(Set.of()), containsInAnyOrder(p, q)),
				() -> assertThat(literal.getInputVariables(Set.of(p, q, r)), containsInAnyOrder(p, q, r)),
				() -> assertThat(literal.getPrivateVariables(Set.of()), containsInAnyOrder(r, s)),
				() -> assertThat(literal.getPrivateVariables(Set.of(p, q, r)), containsInAnyOrder(s))
		);
	}

	@Test
	void repeatedPositiveDirectionTest() {
		var literal = fakeConstraint.call(p, p, q, q);
		assertAll(
				() -> assertThat(literal.getOutputVariables(), containsInAnyOrder(q)),
				() -> assertThat(literal.getInputVariables(Set.of()), containsInAnyOrder(p)),
				() -> assertThat(literal.getInputVariables(Set.of(p, q)), containsInAnyOrder(p)),
				() -> assertThat(literal.getPrivateVariables(Set.of()), empty()),
				() -> assertThat(literal.getPrivateVariables(Set.of(p, q)), empty())
		);
	}

	@Test
	void repeatedNegativeDirectionTest() {
		var literal = not(fakeConstraint.call(p, p, q, q));
		assertAll(
				() -> assertThat(literal.getOutputVariables(), empty()),
				() -> assertThat(literal.getInputVariables(Set.of()), containsInAnyOrder(p)),
				() -> assertThat(literal.getInputVariables(Set.of(p, q)), containsInAnyOrder(p, q)),
				() -> assertThat(literal.getPrivateVariables(Set.of()), containsInAnyOrder(q)),
				() -> assertThat(literal.getPrivateVariables(Set.of(p, q)), empty())
		);
	}
}
