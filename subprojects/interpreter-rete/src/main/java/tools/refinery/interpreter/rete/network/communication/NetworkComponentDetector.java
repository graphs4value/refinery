/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.interpreter.rete.network.communication;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.itc.alg.incscc.IncSCCAlg;
import tools.refinery.interpreter.rete.itc.alg.representative.RepresentativeObserver;
import tools.refinery.interpreter.rete.itc.alg.representative.StronglyConnectedComponentAlgorithm;
import tools.refinery.interpreter.rete.itc.graphimpl.Graph;
import tools.refinery.interpreter.rete.network.Node;

import java.util.Set;

public class NetworkComponentDetector implements RepresentativeObserver<Node> {
	private final Logger logger;
	private final Graph<Node> dependencyGraph;
	private StronglyConnectedComponentAlgorithm<Node> stronglyConnectedComponentAlgorithm;
	private IncSCCAlg<Node> sccInformationProvider;

	public NetworkComponentDetector(Logger logger, Graph<Node> dependencyGraph) {
		this.logger = logger;
		this.dependencyGraph = dependencyGraph;
		stronglyConnectedComponentAlgorithm = new StronglyConnectedComponentAlgorithm<>(dependencyGraph);
		stronglyConnectedComponentAlgorithm.setObserver(this);
		if (stronglyConnectedComponentAlgorithm.getComponents()
				.values()
				.stream()
				.anyMatch(component -> component.size() > 1)) {
			switchToTransitiveClosureAlgorithm();
		}
	}

	@Nullable
	public Set<Node> getPartition(Node node) {
		if (sccInformationProvider == null) {
			return null;
		}
		return sccInformationProvider.sccs.getPartition(node);
	}

	public Node getRepresentative(Node node) {
		if (sccInformationProvider == null) {
			return node;
		}
		return sccInformationProvider.getRepresentative(node);
	}

	public boolean hasOutgoingEdges(Node representative) {
		if (sccInformationProvider == null) {
			return !dependencyGraph.getTargetNodes(representative).isEmpty();
		}
		return sccInformationProvider.hasOutgoingEdges(representative);
	}

	public Graph<Node> getReducedGraph() {
		if (sccInformationProvider == null) {
			return dependencyGraph;
		}
		return sccInformationProvider.getReducedGraph();
	}

	@Override
	public void tupleChanged(Node node, Node representative, Direction direction) {
		if (direction == Direction.INSERT && !node.equals(representative)) {
			switchToTransitiveClosureAlgorithm();
		}
	}

	private void switchToTransitiveClosureAlgorithm() {
		logger.warn("RETE network cycle detected, switching to transitive closure algorithm for communication groups");
		if (stronglyConnectedComponentAlgorithm != null) {
			stronglyConnectedComponentAlgorithm.dispose();
			stronglyConnectedComponentAlgorithm = null;
		}
		if (sccInformationProvider == null) {
			sccInformationProvider = new IncSCCAlg<>(dependencyGraph);
		}
	}
}
