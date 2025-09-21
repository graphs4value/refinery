/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.pquery;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.Query;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.logic.term.int_.IntTerms.add;
import static tools.refinery.logic.term.int_.IntTerms.constant;
import static tools.refinery.logic.tests.QueryMatchers.structurallyEqualTo;

class OptimizationBarrierTest {
	@Test
	void mergeAssignmentTest() {
		var query = Query.of("Actual", Integer.class, (builder, output) -> builder
				.clause(Integer.class, Integer.class, (v1, v2) -> List.of(
						v1.assign(constant(10)),
						v2.assign(constant(10)),
						output.assign(add(v1, v2))
				)));

		var expected = Query.of("Expected", Integer.class, (builder, output) -> builder
				.clause(Integer.class, (v1) -> List.of(
						v1.assign(constant(10)),
						output.assign(add(v1, v1))
				)));

		assertThat(query.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void noMergeAssignmentTest() {
		var query = Query.of("Actual", Integer.class, (builder, output) -> builder
				.clause(Integer.class, Integer.class, (v1, v2) -> List.of(
						v1.assign(new OptimizationBarrier<>(constant(10))),
						v2.assign(new OptimizationBarrier<>(constant(10))),
						output.assign(add(v1, v2))
				)));

		assertThat(query.getDnf().getClauses().getFirst().literals().size(), is(3));
	}
}
