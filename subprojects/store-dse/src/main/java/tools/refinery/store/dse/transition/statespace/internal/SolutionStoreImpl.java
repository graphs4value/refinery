/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.SolutionStore;
import tools.refinery.store.dse.transition.statespace.SolutionStoreListener;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;


public class SolutionStoreImpl implements SolutionStore {
	private final int maxNumberSolutions;

	public static final int UNLIMITED = -1;

	private final PriorityQueue<VersionWithObjectiveValue> solutions;

	private final List<SolutionStoreListener> listeners = new ArrayList<>();

	public SolutionStoreImpl(int maxNumberSolutions) {
		this.maxNumberSolutions = maxNumberSolutions;
		solutions = new PriorityQueue<>(ObjectivePriorityQueueImpl.c1.reversed());
	}

	@Override
	public synchronized boolean submit(VersionWithObjectiveValue version) {
		boolean removeLast = hasEnoughSolution();
		if (!solutions.add(version)) {
			return false;
		}
		if (removeLast) {
			var last = solutions.poll();
			boolean replaced = !version.equals(last);
			if (replaced) {
				solutionRemoved(last);
				solutionAdded(version);
			}
			return replaced;
		} else {
			solutionAdded(version);
			return true;
		}
	}

	private void solutionAdded(VersionWithObjectiveValue versionWithObjectiveValue) {
		for (var listener : listeners) {
			listener.solutionAdded(versionWithObjectiveValue);
		}
	}

	private void solutionRemoved(VersionWithObjectiveValue versionWithObjectiveValue) {
		for (var listener : listeners) {
			listener.solutionRemoved(versionWithObjectiveValue);
		}
	}

	@Override
	public List<VersionWithObjectiveValue> getSolutions() {
		return new ArrayList<>(solutions);
	}

	@Override
	public boolean hasEnoughSolution() {
		if (maxNumberSolutions == UNLIMITED) {
			return false;
		} else {
			return solutions.size() >= maxNumberSolutions;
		}
	}

	@Override
	public void addListener(SolutionStoreListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(SolutionStoreListener listener) {
		listeners.remove(listener);
	}
}
