/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.viatra.runtime.base.itc.alg.representative;

import tools.refinery.viatra.runtime.base.itc.graphimpl.Graph;
import tools.refinery.viatra.runtime.base.itc.igraph.IGraphObserver;
import tools.refinery.viatra.runtime.matchers.util.Direction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class RepresentativeElectionAlgorithm implements IGraphObserver<Object> {
	protected final Graph<Object> graph;
	protected final Map<Object, Object> representatives = new HashMap<>();
	protected final Map<Object, Set<Object>> components = new HashMap<>();
	private RepresentativeObserver observer;

	protected RepresentativeElectionAlgorithm(Graph<Object> graph) {
		this.graph = graph;
		initializeComponents();
		graph.attachObserver(this);
	}

	protected abstract void initializeComponents();

	protected void initializeSet(Set<Object> set) {
		var iterator = set.iterator();
		if (!iterator.hasNext()) {
			// Set is empty.
			return;
		}
		var representative = iterator.next();
		for (var node : set) {
			var oldRepresentative = representatives.put(node, representative);
			if (oldRepresentative != null && !representative.equals(oldRepresentative)) {
				throw new IllegalStateException("Node %s is already in a set represented by %s, cannot add it to %s"
						.formatted(node, oldRepresentative, set));
			}
		}
		components.put(representative, set);
	}

	protected void merge(Object leftRepresentative, Object rightRepresentative) {
		if (leftRepresentative.equals(rightRepresentative)) {
			return;
		}
		var leftSet = getComponent(leftRepresentative);
		var rightSet = getComponent(rightRepresentative);
		if (leftSet.size() < rightSet.size()) {
			merge(rightRepresentative, rightSet, leftRepresentative, leftSet);
		} else {
			merge(leftRepresentative, leftSet, rightRepresentative, rightSet);
		}
	}

	private void merge(Object preservedRepresentative, Set<Object> preservedSet, Object removedRepresentative,
					   Set<Object> removedSet) {
		components.remove(removedRepresentative);
		for (var node : removedSet) {
			representatives.put(node, preservedRepresentative);
			preservedSet.add(node);
			notifyToObservers(node, removedRepresentative, preservedRepresentative);
		}
	}

	protected void assignNewRepresentative(Object oldRepresentative, Set<Object> set) {
		var iterator = set.iterator();
		if (!iterator.hasNext()) {
			return;
		}
		var newRepresentative = iterator.next();
		components.put(newRepresentative, set);
		for (var node : set) {
			var oldRepresentativeOfNode = representatives.put(node, newRepresentative);
			if (!oldRepresentative.equals(oldRepresentativeOfNode)) {
				throw new IllegalArgumentException("Node %s was not represented by %s but by %s"
						.formatted(node, oldRepresentative, oldRepresentativeOfNode));
			}
			notifyToObservers(node, oldRepresentative, newRepresentative);
		}
	}

	public void setObserver(RepresentativeObserver observer) {
		this.observer = observer;
	}

	public Map<Object, Set<Object>> getComponents() {
		return components;
	}

	public Object getRepresentative(Object node) {
		return representatives.get(node);
	}

	public Set<Object> getComponent(Object representative) {
		return components.get(representative);
	}

	public void dispose() {
		graph.detachObserver(this);
	}

	@Override
	public void nodeInserted(Object n) {
		var component = new HashSet<>(1);
		component.add(n);
		initializeSet(component);
		notifyToObservers(n, n, Direction.INSERT);
	}

	@Override
	public void nodeDeleted(Object n) {
		var representative = representatives.remove(n);
		if (!representative.equals(n)) {
			throw new IllegalStateException("Trying to delete node with dangling edges");
		}
		components.remove(representative);
		notifyToObservers(n, representative, Direction.DELETE);
	}

	protected void notifyToObservers(Object node, Object oldRepresentative, Object newRepresentative) {
		notifyToObservers(node, oldRepresentative, Direction.DELETE);
		notifyToObservers(node, newRepresentative, Direction.INSERT);
	}

	protected void notifyToObservers(Object node, Object representative, Direction direction) {
		if (observer != null) {
			observer.tupleChanged(node, representative, direction);
		}
	}

	public interface Factory {
		RepresentativeElectionAlgorithm create(Graph<Object> graph);
	}
}
