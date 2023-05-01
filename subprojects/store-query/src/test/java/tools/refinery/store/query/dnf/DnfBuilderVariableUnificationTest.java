/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.query.view.SymbolView;
import tools.refinery.store.representation.Symbol;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static tools.refinery.store.query.tests.QueryMatchers.structurallyEqualTo;

class DnfBuilderVariableUnificationTest {
	private final Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class, false);
	private final Symbol<Boolean> children = new Symbol<>("friend", 2, Boolean.class, false);
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
		var expected = Dnf.of(builder -> {
			var p = builder.parameter("p");
			builder.clause(friendView.call(p, p));
		});

		assertThat(actual, structurallyEqualTo(expected));
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
		var expected = Dnf.of(builder -> {
			var p = builder.parameter("p");
			builder.clause(friendView.call(p, p));
		});

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void equalQuantifiedTest() {
		var actual = Dnf.of(builder -> builder.clause((p, q) -> List.of(
				friendView.call(p, q),
				p.isEquivalent(q)
		)));
		var expected = Dnf.of(builder -> builder.clause(p -> List.of(friendView.call(p, p))));

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void equalQuantifiedTransitiveTest() {
		var actual = Dnf.of(builder -> builder.clause((p, q, r) -> List.of(
				friendView.call(p, q),
				p.isEquivalent(q),
				childrenView.call(p, r),
				q.isEquivalent(r)
		)));
		var expected = Dnf.of(builder -> builder.clause(p -> List.of(
				friendView.call(p, p),
				childrenView.call(p, p)
		)));

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void equalQuantifiedTransitiveRemoveDuplicateTest() {
		var actual = Dnf.of(builder -> builder.clause((p, q, r) -> List.of(
				friendView.call(p, q),
				p.isEquivalent(q),
				friendView.call(p, r),
				q.isEquivalent(r)
		)));
		var expected = Dnf.of(builder -> builder.clause(p -> List.of(friendView.call(p, p))));

		assertThat(actual, structurallyEqualTo(expected));
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
		var expected = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause(
					q.isEquivalent(p),
					friendView.call(p, p)
			);
		});

		assertThat(actual, structurallyEqualTo(expected));
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
		var expected = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			var r = builder.parameter("r");
			builder.clause(
					q.isEquivalent(p),
					r.isEquivalent(p),
					friendView.call(p, p),
					childrenView.call(p, p)
			);
		});

		assertThat(actual, structurallyEqualTo(expected));
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
		var expected = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause(
					q.isEquivalent(p),
					friendView.call(p, p),
					childrenView.call(p, p)
			);
		});

		assertThat(actual, structurallyEqualTo(expected));
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
		var expected = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause(
					q.isEquivalent(p),
					friendView.call(p, p),
					childrenView.call(p, p)
			);
		});

		assertThat(actual, structurallyEqualTo(expected));
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
		var expected = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause(
					q.isEquivalent(p),
					friendView.call(p, p),
					childrenView.call(p, p)
			);
		});

		assertThat(actual, structurallyEqualTo(expected));
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
		var expected = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause(
					q.isEquivalent(p),
					friendView.call(p, p),
					childrenView.call(p, p)
			);
		});

		assertThat(actual, structurallyEqualTo(expected));
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
		var expected = Dnf.of(builder -> {
			var p = builder.parameter("p");
			var q = builder.parameter("q");
			builder.clause(
					q.isEquivalent(p),
					friendView.call(p, p),
					childrenView.call(p, p)
			);
		});

		assertThat(actual, structurallyEqualTo(expected));
	}
}
