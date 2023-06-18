/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.query.view.SymbolView;
import tools.refinery.store.representation.Symbol;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static tools.refinery.store.query.tests.QueryMatchers.structurallyEqualTo;

class DnfBuilderVariableUnificationTest {
	private final Symbol<Boolean> friend = Symbol.of("friend", 2);
	private final Symbol<Boolean> children = Symbol.of("children", 2);
	private final SymbolView<Boolean> friendView = new KeyOnlyView<>(friend);
	private final SymbolView<Boolean> childrenView = new KeyOnlyView<>(children);

	@Test
	void equalToParameterTest() {
		var actual = Dnf.of(builder -> {
			var p = builder.parameter("p");
			builder.clause(q -> List.of(
					friendView.call(p, q),
					p.isEquivalent(q)
			));
		});

		var expectedP = Variable.of("p");
		assertThat(actual, structurallyEqualTo(
				List.of(new SymbolicParameter(expectedP, ParameterDirection.OUT)),
				List.of(
						List.of(friendView.call(expectedP, expectedP))
				)
		));
	}

	@Test
	void equalToParameterReverseTest() {
		var actual = Dnf.of(builder -> {
			var p = builder.parameter("p");
			builder.clause(q -> List.of(
					friendView.call(p, q),
					q.isEquivalent(p)
			));
		});

		var expectedP = Variable.of("p");
		assertThat(actual, structurallyEqualTo(
				List.of(new SymbolicParameter(expectedP, ParameterDirection.OUT)),
				List.of(
						List.of(friendView.call(expectedP, expectedP))
				)
		));
	}

	@Test
	void equalQuantifiedTest() {
		var actual = Dnf.of(builder -> builder.clause((p, q) -> List.of(
				friendView.call(p, q),
				p.isEquivalent(q)
		)));

		var expectedP = Variable.of("p");
		assertThat(actual, structurallyEqualTo(
				List.of(),
				List.of(
						List.of(friendView.call(expectedP, expectedP))
				)
		));
	}

	@Test
	void equalQuantifiedTransitiveTest() {
		var actual = Dnf.of(builder -> builder.clause((p, q, r) -> List.of(
				friendView.call(p, q),
				p.isEquivalent(q),
				childrenView.call(p, r),
				q.isEquivalent(r)
		)));

		var expectedP = Variable.of("p");
		assertThat(actual, structurallyEqualTo(
				List.of(),
				List.of(
						List.of(friendView.call(expectedP, expectedP), childrenView.call(expectedP, expectedP))
				)
		));
	}

	@Test
	void equalQuantifiedTransitiveRemoveDuplicateTest() {
		var actual = Dnf.of(builder -> builder.clause((p, q, r) -> List.of(
				friendView.call(p, q),
				p.isEquivalent(q),
				friendView.call(p, r),
				q.isEquivalent(r)
		)));

		var expectedP = Variable.of("p");
		assertThat(actual, structurallyEqualTo(
				List.of(),
				List.of(
						List.of(friendView.call(expectedP, expectedP))
				)
		));
	}

	@Test
	void parametersEqualTest() {
		var actual = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause(
					friendView.call(p, q),
					p.isEquivalent(q)
			);
		});

		var expectedP = Variable.of("p");
		var expectedQ = Variable.of("q");
		assertThat(actual, structurallyEqualTo(
				List.of(
						new SymbolicParameter(expectedP, ParameterDirection.OUT),
						new SymbolicParameter(expectedQ, ParameterDirection.OUT)
				),
				List.of(
						List.of(friendView.call(expectedP, expectedP), expectedQ.isEquivalent(expectedP))
				)
		));
	}

	@Test
	void parametersEqualTransitiveTest() {
		var actual = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			var r = builder.parameter("r");
			builder.clause(
					friendView.call(p, q),
					childrenView.call(p, r),
					p.isEquivalent(q),
					r.isEquivalent(q)
			);
		});

		var expectedP = Variable.of("p");
		var expectedQ = Variable.of("q");
		var expectedR = Variable.of("r");
		assertThat(actual, structurallyEqualTo(
				List.of(
						new SymbolicParameter(expectedP, ParameterDirection.OUT),
						new SymbolicParameter(expectedQ, ParameterDirection.OUT),
						new SymbolicParameter(expectedR, ParameterDirection.OUT)
				),
				List.of(
						List.of(
								friendView.call(expectedP, expectedP),
								expectedQ.isEquivalent(expectedP),
								expectedR.isEquivalent(expectedP),
								childrenView.call(expectedP, expectedP)
						)
				)
		));
	}

	@Test
	void parameterAndQuantifiedEqualsTest() {
		var actual = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause((r) -> List.of(
					friendView.call(p, r),
					p.isEquivalent(r),
					childrenView.call(q, r),
					q.isEquivalent(r)
			));
		});


		var expectedP = Variable.of("p");
		var expectedQ = Variable.of("q");
		assertThat(actual, structurallyEqualTo(
				List.of(
						new SymbolicParameter(expectedP, ParameterDirection.OUT),
						new SymbolicParameter(expectedQ, ParameterDirection.OUT)
				),
				List.of(
						List.of(
								friendView.call(expectedP, expectedP),
								expectedQ.isEquivalent(expectedP),
								childrenView.call(expectedP, expectedP)
						)
				)
		));
	}

	@Test
	void parameterAndQuantifiedEqualsReverseFirstTest() {
		var actual = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause((r) -> List.of(
					friendView.call(p, r),
					r.isEquivalent(p),
					childrenView.call(q, r),
					q.isEquivalent(r)
			));
		});

		var expectedP = Variable.of("p");
		var expectedQ = Variable.of("q");
		assertThat(actual, structurallyEqualTo(
				List.of(
						new SymbolicParameter(expectedP, ParameterDirection.OUT),
						new SymbolicParameter(expectedQ, ParameterDirection.OUT)
				),
				List.of(
						List.of(
								friendView.call(expectedP, expectedP),
								expectedQ.isEquivalent(expectedP),
								childrenView.call(expectedP, expectedP)
						)
				)
		));
	}

	@Test
	void parameterAndQuantifiedEqualsReverseSecondTest() {
		var actual = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause((r) -> List.of(
					friendView.call(p, r),
					p.isEquivalent(r),
					childrenView.call(q, r),
					r.isEquivalent(q)
			));
		});

		var expectedP = Variable.of("p");
		var expectedQ = Variable.of("q");
		assertThat(actual, structurallyEqualTo(
				List.of(
						new SymbolicParameter(expectedP, ParameterDirection.OUT),
						new SymbolicParameter(expectedQ, ParameterDirection.OUT)
				),
				List.of(
						List.of(
								friendView.call(expectedP, expectedP),
								expectedQ.isEquivalent(expectedP),
								childrenView.call(expectedP, expectedP)
						)
				)
		));
	}

	@Test
	void parameterAndQuantifiedEqualsReverseBoth() {
		var actual = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause((r) -> List.of(
					friendView.call(p, r),
					p.isEquivalent(r),
					childrenView.call(q, r),
					r.isEquivalent(q)
			));
		});

		var expectedP = Variable.of("p");
		var expectedQ = Variable.of("q");
		assertThat(actual, structurallyEqualTo(
				List.of(
						new SymbolicParameter(expectedP, ParameterDirection.OUT),
						new SymbolicParameter(expectedQ, ParameterDirection.OUT)
				),
				List.of(
						List.of(
								friendView.call(expectedP, expectedP),
								expectedQ.isEquivalent(expectedP),
								childrenView.call(expectedP, expectedP)
						)
				)
		));
	}

	@Test
	void parameterAndTwoQuantifiedEqualsTest() {
		var actual = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause((r, s) -> List.of(
					r.isEquivalent(s),
					friendView.call(p, r),
					p.isEquivalent(r),
					childrenView.call(q, s),
					q.isEquivalent(s)
			));
		});

		var expectedP = Variable.of("p");
		var expectedQ = Variable.of("q");
		assertThat(actual, structurallyEqualTo(
				List.of(
						new SymbolicParameter(expectedP, ParameterDirection.OUT),
						new SymbolicParameter(expectedQ, ParameterDirection.OUT)
				),
				List.of(
						List.of(
								friendView.call(expectedP, expectedP),
								expectedQ.isEquivalent(expectedP),
								childrenView.call(expectedP, expectedP)
						)
				)
		));
	}
}
