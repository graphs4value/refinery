/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.rewriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.literal.AbstractCallLiteral;
import tools.refinery.logic.literal.Reduction;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.tests.FakeKeyOnlyView;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static tools.refinery.logic.literal.Literals.not;

class DuplicateDnfRemoverTest {
	private final static Constraint friendView = new FakeKeyOnlyView("friend", 2);

	private DuplicateDnfRemover sut;

	@BeforeEach
	void beforeEach() {
		sut = new DuplicateDnfRemover();
	}

	@Test
	void removeDuplicateSimpleTest() {
		var one = Query.of("One", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));
		var two = Query.of("Two", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));

		var oneResult = sut.rewrite(one);
		var twoResult = sut.rewrite(two);

		assertThat(oneResult, is(twoResult));
		assertThat(one, is(oneResult));
	}

	@Test
	void notDuplicateSimpleTest() {
		var one = Query.of("One", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));
		var two = Query.of("Two", (builder, x, y) -> builder.clause((z) -> List.of(
				friendView.call(x, y),
				friendView.call(y, z)
		)));

		var oneResult = sut.rewrite(one);
		var twoResult = sut.rewrite(two);

		assertThat(one, is(oneResult));
		assertThat(two, is(twoResult));
	}

	@Test
	void removeDuplicateRecursiveTest() {
		var oneSubQuery = Query.of("OneSubQuery", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));
		var one = Query.of("One", (builder, x) -> builder.clause(
				oneSubQuery.call(x, Variable.of())
		));
		var twoSubQuery = Query.of("TwoSubQuery", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));
		var two = Query.of("Two", (builder, x) -> builder.clause(
				twoSubQuery.call(x, Variable.of())
		));

		var oneResult = sut.rewrite(one);
		var twoResult = sut.rewrite(two);

		assertThat(oneResult, is(twoResult));
		assertThat(one, is(oneResult));
	}

	@Test
	void notDuplicateRecursiveTest() {
		var oneSubQuery = Query.of("OneSubQuery", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));
		var one = Query.of("One", (builder, x) -> builder.clause(
				oneSubQuery.call(x, Variable.of())
		));
		var twoSubQuery = Query.of("TwoSubQuery", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));
		var two = Query.of("Two", (builder, x) -> builder.clause(
				twoSubQuery.call(Variable.of(), x)
		));

		var oneResult = sut.rewrite(one);
		var twoResult = sut.rewrite(two);

		assertThat(one, is(oneResult));
		assertThat(oneResult, is(not(twoResult)));

		var oneCall = (AbstractCallLiteral) oneResult.getDnf().getClauses().getFirst().literals().getFirst();
		var twoCall = (AbstractCallLiteral) twoResult.getDnf().getClauses().getFirst().literals().getFirst();

		assertThat(oneCall.getTarget(), is(twoCall.getTarget()));
	}

	@Test
	void removeContradictionTest() {
		var oneSubQuery = Query.of("OneSubQuery", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));
		var twoSubQuery = Query.of("TwoSubQuery", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));
		var query = Query.of("Contradiction", (builder, x, y) -> builder.clause(
				oneSubQuery.call(x, y),
				not(twoSubQuery.call(x, y))
		));

		var result = sut.rewrite(query);

		assertThat(result.getDnf().getReduction(), is(Reduction.ALWAYS_FALSE));
	}

	@Test
	void removeQuantifiedContradictionTest() {
		var oneSubQuery = Query.of("OneSubQuery", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));
		var twoSubQuery = Query.of("TwoSubQuery", (builder, x, y) -> builder.clause(
				friendView.call(x, y),
				friendView.call(y, x)
		));
		var query = Query.of("Contradiction", (builder, x) -> builder.clause(
				oneSubQuery.call(x, Variable.of()),
				not(twoSubQuery.call(x, Variable.of()))
		));

		var result = sut.rewrite(query);

		assertThat(result.getDnf().getReduction(), is(Reduction.ALWAYS_FALSE));
	}
}
