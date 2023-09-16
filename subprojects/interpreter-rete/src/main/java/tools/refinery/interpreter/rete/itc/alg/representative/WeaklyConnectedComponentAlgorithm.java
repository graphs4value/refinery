/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.interpreter.rete.itc.alg.representative;

import tools.refinery.interpreter.rete.itc.graphimpl.Graph;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class WeaklyConnectedComponentAlgorithm extends RepresentativeElectionAlgorithm {
	public WeaklyConnectedComponentAlgorithm(Graph<Object> graph) {
		super(graph);
	}

	@Override
	protected void initializeComponents() {
		for (var node : graph.getAllNodes()) {
			if (representatives.containsKey(node)) {
				continue;
			}
			var reachable = getReachableNodes(node);
			initializeSet(reachable);
		}
	}

	@Override
	public void edgeInserted(Object source, Object target) {
		var sourceRoot = getRepresentative(source);
		var targetRoot = getRepresentative(target);
		merge(sourceRoot, targetRoot);
	}

	@Override
	public void edgeDeleted(Object source, Object target) {
		var sourceRoot = getRepresentative(source);
		var targetRoot = getRepresentative(target);
		if (!sourceRoot.equals(targetRoot)) {
			throw new IllegalArgumentException("Trying to remove edge not in graph");
		}
		var targetReachable = getReachableNodes(target);
		if (!targetReachable.contains(source)) {
			split(sourceRoot, targetReachable);
		}
	}

	private void split(Object sourceRepresentative, Set<Object> targetReachable) {
		var sourceComponent = getComponent(sourceRepresentative);
		sourceComponent.removeAll(targetReachable);
		if (targetReachable.contains(sourceRepresentative)) {
			components.put(sourceRepresentative, targetReachable);
			assignNewRepresentative(sourceRepresentative, sourceComponent);
		} else {
			assignNewRepresentative(sourceRepresentative, targetReachable);
		}
	}

	private Set<Object> getReachableNodes(Object source) {
		var retSet = new HashSet<>();
		retSet.add(source);
		var nodeQueue = new ArrayDeque<>();
		nodeQueue.addLast(source);

		while (!nodeQueue.isEmpty()) {
			var node = nodeQueue.removeFirst();
			for (var neighbor : graph.getTargetNodes(node).distinctValues()) {
				if (!retSet.contains(neighbor)) {
					retSet.add(neighbor);
					nodeQueue.addLast(neighbor);
				}
			}
			for (var neighbor : graph.getSourceNodes(node).distinctValues()) {
				if (!retSet.contains(neighbor)) {
					retSet.add(neighbor);
					nodeQueue.addLast(neighbor);
				}
			}
		}

		return retSet;
	}
}
