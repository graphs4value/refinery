/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.ObjectiveValues;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.dse.transition.statespace.ObjectivePriorityQueue;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class ObjectivePriorityQueueImpl implements ObjectivePriorityQueue {
	public static final Comparator<VersionWithObjectiveValue> c1 = (o1, o2) -> Double.compare(
			((ObjectiveValues.ObjectiveValue1) o1.objectiveValue()).value0(),
			((ObjectiveValues.ObjectiveValue1) o2.objectiveValue()).value0());
	// TODO: support multi objective!
	final PriorityQueue<VersionWithObjectiveValue> priorityQueue;

	public ObjectivePriorityQueueImpl(List<Objective> objectives) {

		if(objectives.size() == 1) {
			this.priorityQueue = new PriorityQueue<>(c1);
		} else {
			throw new UnsupportedOperationException("Only single objective comparator is implemented currently!");
		}
	}
	@Override
	public Comparator<VersionWithObjectiveValue> getComparator() {
		return c1;
	}

	@Override
	public synchronized void submit(VersionWithObjectiveValue versionWithObjectiveValue) {
		priorityQueue.add(versionWithObjectiveValue);
	}

	@Override
	public synchronized void remove(VersionWithObjectiveValue versionWithObjectiveValue) {
		priorityQueue.remove(versionWithObjectiveValue);
	}

	@Override
	public synchronized int getSize() {
		return priorityQueue.size();
	}

	@Override
	public synchronized VersionWithObjectiveValue getBest() {
		return priorityQueue.peek();
	}

	@Override
	public synchronized VersionWithObjectiveValue getRandom(Random random) {
		int randomPosition = random.nextInt(getSize());
		for (VersionWithObjectiveValue entry : this.priorityQueue) {
			if (randomPosition-- == 0) {
				return entry;
			}
		}
		throw new IllegalStateException("The priority queue is inconsistent!");
	}
}
