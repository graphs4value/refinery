/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.InvalidClauseException;
import tools.refinery.store.query.term.*;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.INT_SUM;
import static tools.refinery.store.query.term.int_.IntTerms.constant;

class AggregationLiteralTest {
	private static final NodeVariable p = Variable.of("p");
	private static final DataVariable<Integer> x = Variable.of("x", Integer.class);
	private static final DataVariable<Integer> y = Variable.of("y", Integer.class);
	private static final DataVariable<Integer> z = Variable.of("z", Integer.class);
	private static final Constraint fakeConstraint = new Constraint() {
		@Override
		public String name() {
			return getClass().getName();
		}

		@Override
		public List<Parameter> getParameters() {
			return List.of(
					new Parameter(null, ParameterDirection.OUT),
					new Parameter(Integer.class, ParameterDirection.OUT)
			);
		}
	};

	@Test
	void parameterDirectionTest() {
		var literal = x.assign(fakeConstraint.aggregateBy(y, INT_SUM, p, y));
		assertAll(
				() -> assertThat(literal.getOutputVariables(), containsInAnyOrder(x)),
				() -> assertThat(literal.getInputVariables(Set.of()), empty()),
				() -> assertThat(literal.getInputVariables(Set.of(p)), containsInAnyOrder(p)),
				() -> assertThat(literal.getPrivateVariables(Set.of()), containsInAnyOrder(p, y)),
				() -> assertThat(literal.getPrivateVariables(Set.of(p)), containsInAnyOrder(y))
		);
	}

	@Test
	void missingAggregationVariableTest() {
		var aggregation = fakeConstraint.aggregateBy(y, INT_SUM, p, z);
		assertThrows(InvalidQueryException.class, () -> x.assign(aggregation));
	}

	@Test
	void circularAggregationVariableTest() {
		var aggregation = fakeConstraint.aggregateBy(x, INT_SUM, p, x);
		assertThrows(InvalidQueryException.class, () -> x.assign(aggregation));
	}

	@Test
	void unboundTwiceVariableTest() {
		var builder = Dnf.builder()
				.clause(
						not(fakeConstraint.call(p, y)),
						x.assign(fakeConstraint.aggregateBy(y, INT_SUM, p, y))
				);
		assertThrows(InvalidClauseException.class, builder::build);
	}

	@Test
	void unboundBoundVariableTest() {
		var builder = Dnf.builder()
				.clause(
						y.assign(constant(27)),
						x.assign(fakeConstraint.aggregateBy(y, INT_SUM, p, y))
				);
		assertThrows(InvalidClauseException.class, builder::build);
	}
}
