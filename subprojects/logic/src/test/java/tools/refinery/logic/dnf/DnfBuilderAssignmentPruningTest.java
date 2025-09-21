/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.tests.FakeFunctionView;
import tools.refinery.logic.tests.FakeKeyOnlyView;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.term.int_.IntTerms.*;
import static tools.refinery.logic.tests.QueryMatchers.structurallyEqualTo;

class DnfBuilderAssignmentPruningTest {
	private final Constraint personView = new FakeKeyOnlyView("Person", 1);
	private final Constraint ageView = new FakeFunctionView<>("age", 1, Integer.class);

	@Test
	void noPruningTest() {
		var query = Query.of("Actual", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, (v1, v2) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						output.assign(add(v1, constant(10)))
				)));

		assertThat(query.getDnf(), structurallyEqualTo(query.getDnf()));
	}

	@Test
	void simplePruningTest() {
		var query = Query.of("Actual", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, Integer.class, (v1, v2, v3, v4) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						v3.assign(mul(ageView.leftJoinBy(v4, 0, p1, v4), constant(2))),
						output.assign(add(v1, v3))
				)));

		var expected = Query.of("Expected", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, (v1, v2) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						output.assign(add(v1, v1))
				)));

		assertThat(query.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void parameterTest() {
		var query = Query.of("Actual", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, (v1, v2, v3) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						check(lessEq(v1, constant(100))),
						output.assign(mul(ageView.leftJoinBy(v3, 0, p1, v3), constant(2)))
				)));

		// Do not prune assignments to parameter variables, as this would change query semantics.
		var expected = Query.of("Actual", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, (v1, v2, v3) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						check(lessEq(v1, constant(100))),
						output.assign(mul(ageView.leftJoinBy(v3, 0, p1, v3), constant(2)))
				)));

		assertThat(query.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void duplicateAssignmentTest() {
		var query = Query.of("Actual", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, (v1, v2, v3) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						v1.assign(mul(ageView.leftJoinBy(v3, 0, p1, v3), constant(3))),
						output.assign(add(v1, constant(10)))
				)));

		assertThat(query.getDnf(), structurallyEqualTo(query.getDnf()));
	}

	@Test
	void duplicateAssignmentRemovalTest() {
		var query = Query.of("Actual", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, (v1, v2, v3) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						v1.assign(mul(ageView.leftJoinBy(v3, 0, p1, v3), constant(2))),
						output.assign(add(v1, constant(10)))
				)));

		var expected = Query.of("Expected", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, (v1, v2) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						output.assign(add(v1, constant(10)))
				)));

		assertThat(query.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void mutualAssignmentTest() {
		var v5 = Variable.of("v5", Integer.class);
		var v6 = Variable.of("v5", Integer.class);
		var query = Query.of("Actual", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, Integer.class, (v1, v2, v3, v4) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						v3.assign(mul(ageView.leftJoinBy(v4, 0, p1, v4), constant(2))),
						v3.assign(mul(ageView.leftJoinBy(v5, 0, p1, v5), constant(3))),
						v1.assign(mul(ageView.leftJoinBy(v6, 0, p1, v6), constant(3))),
						output.assign(add(v1, v3))
				)));

		var expected = Query.of("Expected", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, (v1, v2, v3) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						v1.assign(mul(ageView.leftJoinBy(v3, 0, p1, v3), constant(3))),
						output.assign(add(v1, v1))
				)));

		assertThat(query.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void iterativePruningTest() {
		var v5 = Variable.of("v5", Integer.class);
		var v6 = Variable.of("v5", Integer.class);
		var query = Query.of("Actual", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, Integer.class, (v1, v2, v3, v4) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						v3.assign(mul(ageView.leftJoinBy(v4, 0, p1, v4), constant(2))),
						v5.assign(add(v1, constant(10))),
						v6.assign(add(v3, constant(10))),
						output.assign(add(v5, v6))
				)));

		var expected = Query.of("Expected", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, (v1, v2, v3) -> List.of(
						personView.call(p1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						v3.assign(add(v1, constant(10))),
						output.assign(add(v3, v3))
				)));

		assertThat(query.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

	@Test
	void alreadyBoundTest() {
		var query = Query.of("Actual", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, Integer.class, (v1, v2, v3, v4) -> List.of(
						personView.call(p1),
						ageView.call(p1, v3),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						v3.assign(mul(ageView.leftJoinBy(v4, 0, p1, v4), constant(2))),
						output.assign(add(v1, v3))
				)));

		var expected = Query.of("Expected", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, (v1, v2) -> List.of(
						personView.call(p1),
						ageView.call(p1, v1),
						v1.assign(mul(ageView.leftJoinBy(v2, 0, p1, v2), constant(2))),
						output.assign(add(v1, v1))
				)));

		assertThat(query.getDnf(), structurallyEqualTo(expected.getDnf()));
	}

}
