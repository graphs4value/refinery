/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.resultset;

import java.util.PriorityQueue;

public class PriorityAgenda {
	private final PriorityQueue<PriorityResultSet<?>> priorityQueue = new PriorityQueue<>(
			(a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

	void addResultSet(PriorityResultSet<?> resultSet) {
        priorityQueue.add(resultSet);
    }

	void removeResultSet(PriorityResultSet<?> resultSet) {
		priorityQueue.remove(resultSet);
	}

	public int getHighestPriority() {
		// The {@code priorityQueue} doesn't own the result sets, so there is no need to close it here.
		@SuppressWarnings("resource")
		var head = priorityQueue.peek();
		return head == null ? Integer.MIN_VALUE : head.getPriority();
	}

	public boolean isEnabled(int priority) {
		return priority >= getHighestPriority();
	}
}
