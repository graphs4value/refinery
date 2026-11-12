/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.store.reasoning.smt.SmtSolverStrategy;
import tools.refinery.z3.Z3SolverLoader;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class AssertionJournalTest {
	@ParameterizedTest
	@EnumSource(SmtSolverStrategy.class)
	void factoryStrategyReplaysSnapshotAfterReset(SmtSolverStrategy selection) {
		Z3SolverLoader.loadNativeLibraries();
		try (var context = new Context()) {
			var solver = context.mkSolver();
			var strategy = selection.create(context, solver);
			var x = context.mkBoolConst("x");
			var assertions = Map.of("first", new SolverAssertion("first", null, List.of(x)));
			strategy.synchronize(Map.copyOf(assertions), Set.of("first"));
			assertEquals(Status.SATISFIABLE, strategy.check());
			strategy.reset();
			assertEquals(0, solver.getNumAssertions());
			strategy.synchronize(Map.copyOf(assertions), Set.of("first"));
			assertEquals(Status.SATISFIABLE, strategy.check());
			assertTrue(solver.getModel().eval(x, true).isTrue());
			strategy.synchronize(Map.of("second", new SolverAssertion("second", null,
					List.of(context.mkNot(x)))), Set.of("first", "second"));
			assertEquals(Status.SATISFIABLE, strategy.check());
			assertTrue(solver.getModel().eval(x, true).isFalse());
		}
	}

	@ParameterizedTest
	@EnumSource(SmtSolverStrategy.class)
	void randomSnapshotsMatchReset(SmtSolverStrategy strategy) {
		Z3SolverLoader.loadNativeLibraries();
		try (var context = new Context()) {
			var solver = context.mkSolver();
			var journal = new AssertionJournal(context, solver, strategy);
			var reference = context.mkSolver();
			var x = context.mkIntConst("x");
			var y = context.mkRealConst("y");
			var b = context.mkBoolConst("b");
			var string = context.mkConst("s", context.getStringSort());
			var pool = new ArrayList<Expr<BoolSort>>();
			for (int i = -5; i <= 5; i++) {
				pool.add(context.mkGe(x, context.mkInt(i)));
				pool.add(context.mkLe(x, context.mkInt(i)));
				pool.add(context.mkGe(y, context.mkReal(i)));
				pool.add(context.mkLe(y, context.mkReal(i)));
			}
			pool.add(b);
			pool.add(context.mkNot(b));
			pool.add(context.mkEq(string, context.mkString("a")));
			pool.add(context.mkEq(string, context.mkString("b")));
			pool.add(context.mkFalse());
			var random = new Random(0);
			var active = new LinkedHashSet<Integer>();
			var snapshots = new ArrayList<Set<Integer>>();
			snapshots.add(Set.of());
			for (int i = 0; i < 2000; i++) {
				if (random.nextInt(4) == 0) {
					active = new LinkedHashSet<>(snapshots.get(random.nextInt(snapshots.size())));
				}
				int key = random.nextInt(pool.size());
				if (!active.add(key)) {
					active.remove(key);
				}
				snapshots.add(Set.copyOf(active));
				for (int j = 0; j < pool.size(); j++) {
					if (active.contains(j)) {
						journal.put(j, null, List.of(pool.get(j)));
					} else {
						journal.remove(j);
					}
				}
				reference.reset();
				reference.add(active.stream().map(pool::get).toArray(Expr[]::new));
				var expected = reference.check();
				assertEquals(expected, journal.check(), "step " + i);
				if (expected == Status.SATISFIABLE) {
					var model = solver.getModel();
					for (int index : active) {
						assertTrue(model.eval(pool.get(index), true).isTrue());
					}
				}
			}
		}
	}

	@ParameterizedTest
	@EnumSource(SmtSolverStrategy.class)
	void narrowingWideningRemovalAndCompaction(SmtSolverStrategy strategy) {
		Z3SolverLoader.loadNativeLibraries();
		try (var context = new Context()) {
			var solver = context.mkSolver();
			var journal = new AssertionJournal(context, solver, strategy);
			var x = context.mkIntConst("x");
			for (int i = 0; i <= 1100; i++) {
				journal.put("domain", IntInterval.of(i, 5000),
						List.of(context.mkGe(x, context.mkInt(i)), context.mkLe(x, context.mkInt(5000))));
				assertEquals(Status.SATISFIABLE, journal.check());
				if (strategy == SmtSolverStrategy.POP_REPLAY && i == 1) {
					assertEquals(2, solver.getNumScopes());
				}
			}
			assertTrue(solver.getNumAssertions() < 2200);
			journal.put("domain", IntInterval.UNKNOWN, List.of());
			journal.put("rule", null, List.of(context.mkEq(x, context.mkInt(-1))));
			assertEquals(Status.SATISFIABLE, journal.check());
			journal.put("domain", IntInterval.ERROR, List.of(context.mkFalse()));
			journal.put("other", IntInterval.ERROR, List.of(context.mkFalse()));
			assertEquals(Status.UNSATISFIABLE, journal.check());
			journal.remove("domain");
			assertEquals(Status.UNSATISFIABLE, journal.check());
			journal.remove("other");
			assertEquals(Status.SATISFIABLE, journal.check());
		}
	}

	@ParameterizedTest
	@EnumSource(SmtSolverStrategy.class)
	void replaysValidSuffixAndCoalescesChanges(SmtSolverStrategy strategy) {
		Z3SolverLoader.loadNativeLibraries();
		try (var context = new Context()) {
			var solver = context.mkSolver();
			var journal = new AssertionJournal(context, solver, strategy);
			for (int i = 0; i < 100; i++) {
				journal.put(i, null, List.of(context.mkBoolConst("a" + i)));
				journal.check();
			}
			int scopes = solver.getNumScopes();
			journal.remove(0);
			journal.put(0, null, List.of(context.mkBoolConst("a0")));
			assertEquals(Status.SATISFIABLE, journal.check());
			assertEquals(scopes, solver.getNumScopes());
			journal.remove(0);
			journal.put("conflict", null, List.of(context.mkNot(context.mkBoolConst("a99"))));
			assertEquals(Status.UNSATISFIABLE, journal.check());
			journal.remove("conflict");
			assertEquals(Status.SATISFIABLE, journal.check());
		}
	}
}

