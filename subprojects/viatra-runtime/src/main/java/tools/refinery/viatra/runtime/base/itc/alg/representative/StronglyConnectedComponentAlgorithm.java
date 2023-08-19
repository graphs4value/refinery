/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.viatra.runtime.base.itc.alg.representative;

import tools.refinery.viatra.runtime.base.itc.alg.misc.GraphHelper;
import tools.refinery.viatra.runtime.base.itc.alg.misc.bfs.BFS;
import tools.refinery.viatra.runtime.base.itc.alg.misc.scc.SCC;
import tools.refinery.viatra.runtime.base.itc.graphimpl.Graph;

import java.util.Collection;
import java.util.Set;

public class StronglyConnectedComponentAlgorithm extends RepresentativeElectionAlgorithm {
	public StronglyConnectedComponentAlgorithm(Graph<Object> graph) {
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
	public void edgeInserted(Object source, Object target) {
		var sourceRoot = getRepresentative(source);
		var targetRoot = getRepresentative(target);
		if (sourceRoot.equals(targetRoot)) {
			// New edge does not change strongly connected components.
			return;
		}
		if (BFS.isReachable(target, source, graph)) {
			merge(sourceRoot, targetRoot);
		}
	}

	@Override
	public void edgeDeleted(Object source, Object target) {
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

	private void split(Object preservedRepresentative, Collection<? extends Set<Object>> sets) {
		for (var set : sets) {
			if (set.contains(preservedRepresentative)) {
				components.put(preservedRepresentative, set);
			} else {
				assignNewRepresentative(preservedRepresentative, set);
			}
		}
	}
}