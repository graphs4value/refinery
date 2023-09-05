/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.SolutionStore;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class SolutionStoreImpl implements SolutionStore {
	final int maxNumberSolutions;
	public static final int UNLIMITED = -1;
	final SortedSet<VersionWithObjectiveValue> solutions;

	public SolutionStoreImpl(int maxNumberSolutions) {
		this.maxNumberSolutions = maxNumberSolutions;
		solutions = new TreeSet<>(ObjectivePriorityQueueImpl.c1);
	}


	@Override
	public synchronized boolean submit(VersionWithObjectiveValue version) {
		boolean removeLast = hasEnoughSolution();
		solutions.add(version);
		if(removeLast) {
			var last = solutions.last();
			solutions.remove(last);
			return last != version;
		} else {
			return true;
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
}
