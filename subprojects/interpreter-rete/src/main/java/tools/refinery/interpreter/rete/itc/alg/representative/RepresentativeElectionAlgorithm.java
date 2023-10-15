/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.interpreter.rete.itc.alg.representative;

import tools.refinery.interpreter.rete.itc.graphimpl.Graph;
import tools.refinery.interpreter.rete.itc.igraph.IGraphObserver;
import tools.refinery.interpreter.matchers.util.Direction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class RepresentativeElectionAlgorithm<T> implements IGraphObserver<T> {
	protected final Graph<T> graph;
	protected final Map<T, T> representatives = new HashMap<>();
	protected final Map<T, Set<T>> components = new HashMap<>();
	private RepresentativeObserver<? super T> observer;

	protected RepresentativeElectionAlgorithm(Graph<T> graph) {
		this.graph = graph;
		initializeComponents();
		graph.attachObserver(this);
	}

	protected abstract void initializeComponents();

	protected void initializeSet(Set<T> set) {
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

	protected void merge(Set<T> toMerge) {
		if (toMerge.isEmpty()) {
			return;
		}
		var representativesToMerge = new HashSet<T>();
		T bestRepresentative = null;
		Set<T> bestSet = null;
		for (var object : toMerge) {
			var representative = getRepresentative(object);
			if (representativesToMerge.add(representative)) {
				var component = getComponent(representative);
				if (bestSet == null || bestSet.size() < component.size()) {
					bestRepresentative = representative;
					bestSet = component;
				}
			}
		}
		if (bestRepresentative == null) {
			throw new AssertionError("Could not determine best representative");
		}
		for (var representative : representativesToMerge) {
			if (!bestRepresentative.equals(representative)) {
				components.remove(representative);
			}
		}
		components.put(bestRepresentative, toMerge);
		for (var object : toMerge) {
			var previousRepresentative = representatives.put(object, bestRepresentative);
			if (!bestSet.contains(object)) {
				notifyToObservers(object, previousRepresentative, bestRepresentative);
			}
		}
	}

	protected void merge(T leftRepresentative, T rightRepresentative) {
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

	private void merge(T preservedRepresentative, Set<T> preservedSet, T removedRepresentative, Set<T> removedSet) {
		components.remove(removedRepresentative);
		for (var node : removedSet) {
			representatives.put(node, preservedRepresentative);
			preservedSet.add(node);
			notifyToObservers(node, removedRepresentative, preservedRepresentative);
		}
	}

	protected void assignNewRepresentative(T oldRepresentative, Set<T> set) {
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

	public void setObserver(RepresentativeObserver<? super T> observer) {
		this.observer = observer;
	}

	public Map<T, Set<T>> getComponents() {
		return components;
	}

	public T getRepresentative(T node) {
		return representatives.get(node);
	}

	public Set<T> getComponent(T representative) {
		return components.get(representative);
	}

	public void dispose() {
		graph.detachObserver(this);
	}

	@Override
	public void nodeInserted(T n) {
		var component = new HashSet<T>(1);
		component.add(n);
		initializeSet(component);
		notifyToObservers(n, n, Direction.INSERT);
	}

	@Override
	public void nodeDeleted(T n) {
		var representative = representatives.remove(n);
		if (!representative.equals(n)) {
			throw new IllegalStateException("Trying to delete node with dangling edges");
		}
		components.remove(representative);
		notifyToObservers(n, representative, Direction.DELETE);
	}

	protected void notifyToObservers(T node, T oldRepresentative, T newRepresentative) {
		notifyToObservers(node, oldRepresentative, Direction.DELETE);
		notifyToObservers(node, newRepresentative, Direction.INSERT);
	}

	protected void notifyToObservers(T node, T representative, Direction direction) {
		if (observer != null) {
			observer.tupleChanged(node, representative, direction);
		}
	}

	@FunctionalInterface
	public interface Factory {
		<T> RepresentativeElectionAlgorithm<T> create(Graph<T> graph);
	}
}
