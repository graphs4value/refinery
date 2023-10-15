/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.interpreter.rete.itc.alg.representative;

import tools.refinery.interpreter.rete.itc.graphimpl.Graph;
import tools.refinery.interpreter.rete.itc.alg.misc.GraphHelper;
import tools.refinery.interpreter.rete.itc.alg.misc.bfs.BFS;
import tools.refinery.interpreter.rete.itc.alg.misc.scc.SCC;

import java.util.Collection;
import java.util.Set;

public class StronglyConnectedComponentAlgorithm<T> extends RepresentativeElectionAlgorithm<T> {
	public StronglyConnectedComponentAlgorithm(Graph<T> graph) {
		super(graph);
	}

	@Override
	protected void initializeComponents() {
		var computedSCCs = SCC.computeSCC(graph).getSccs();
		for (var computedSCC : computedSCCs) {
			initializeSet(computedSCC);
		}
	}

	@Override
	public void edgeInserted(T source, T target) {
		var sourceRoot = getRepresentative(source);
		var targetRoot = getRepresentative(target);
		if (sourceRoot.equals(targetRoot)) {
			// New edge does not change strongly connected components.
			return;
		}
		if (BFS.isReachable(target, source, graph)) {
			var sources = BFS.reachableSources(graph, target);
			var targets = BFS.reachableTargets(graph, source);
			sources.retainAll(targets);
			merge(sources);
		}
	}

	@Override
	public void edgeDeleted(T source, T target) {
		var sourceRoot = getRepresentative(source);
		var targetRoot = getRepresentative(target);
		if (!sourceRoot.equals(targetRoot)) {
			// New edge does not change strongly connected components.
			return;
		}
		var component = GraphHelper.getSubGraph(getComponent(sourceRoot), graph);
		if (!BFS.isReachable(source, target, component)) {
			var newSCCs = SCC.computeSCC(component).getSccs();
			split(sourceRoot, newSCCs);
		}
	}

	private void split(T preservedRepresentative, Collection<? extends Set<T>> sets) {
		for (var set : sets) {
			if (set.contains(preservedRepresentative)) {
				components.put(preservedRepresentative, set);
			} else {
				assignNewRepresentative(preservedRepresentative, set);
			}
		}
	}
}
