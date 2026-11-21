/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;

import java.util.*;

public final class PopReplaySolverStrategy implements SolverStrategy {
	private static final int HISTORY_LIMIT = 1024;
	private final Solver solver;
	private final List<List<SolverAssertion>> frames = new ArrayList<>();
	private final Map<Object, List<ScopedAssertion>> sourceFrames = new HashMap<>();
	private final Set<SolverAssertion> encoded = new HashSet<>();

	private record ScopedAssertion(int index, SolverAssertion assertion) {
	}

	public PopReplaySolverStrategy(Solver solver) {
		this.solver = solver;
	}

	@Override
	public void synchronize(Map<Object, SolverAssertion> desired, Set<Object> dirty) {
		if (frames.size() >= HISTORY_LIMIT || encoded.size() > Math.max(HISTORY_LIMIT, desired.size() * 4L)) {
			reset();
			dirty = desired.keySet();
		}
		int cut = frames.size();
		for (var source : dirty) {
			var indices = sourceFrames.get(source);
			if (indices == null) {
				continue;
			}
			for (var scoped : indices) {
				if (scoped.index() >= cut) {
					break;
				}
				if (!scoped.assertion().isImpliedBy(desired.get(source))) {
					cut = scoped.index();
					break;
				}
			}
		}
		var pending = new LinkedHashSet<>(dirty);
		if (cut < frames.size()) {
			solver.pop(frames.size() - cut);
			for (int i = frames.size() - 1; i >= cut; i--) {
				for (var assertion : frames.remove(i)) {
					encoded.remove(assertion);
					pending.add(assertion.source());
					var indices = sourceFrames.get(assertion.source());
					indices.removeLast();
					if (indices.isEmpty()) {
						sourceFrames.remove(assertion.source());
					}
				}
			}
		}
		var additions = new ArrayList<SolverAssertion>();
		for (var source : pending) {
			var assertion = desired.get(source);
			if (assertion != null && !assertion.expressions().isEmpty() && !encoded.contains(assertion)) {
				additions.add(assertion);
			}
		}
		if (additions.isEmpty()) {
			return;
		}
		solver.push();
		int index = frames.size();
		for (var assertion : additions) {
			add(assertion);
			encoded.add(assertion);
			sourceFrames.computeIfAbsent(assertion.source(), ignored -> new ArrayList<>())
					.add(new ScopedAssertion(index, assertion));
		}
		frames.add(additions);
	}

	@Override
	public Status check() {
		return solver.check();
	}

	@Override
	public void reset() {
		solver.reset();
		frames.clear();
		sourceFrames.clear();
		encoded.clear();
	}

	@SuppressWarnings("unchecked")
	private void add(SolverAssertion assertion) {
		solver.add(assertion.expressions().toArray(Expr[]::new));
	}
}
