/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.single;

import tools.refinery.interpreter.rete.network.ReinitializedNode;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.itc.alg.representative.RepresentativeElectionAlgorithm;
import tools.refinery.interpreter.rete.itc.alg.representative.RepresentativeObserver;
import tools.refinery.interpreter.rete.itc.graphimpl.Graph;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.Clearable;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

import java.util.Collection;
import java.util.Map;

public class RepresentativeElectionNode extends SingleInputNode implements Clearable, RepresentativeObserver<Object>,
        ReinitializedNode {
	private final RepresentativeElectionAlgorithm.Factory algorithmFactory;
	private Graph<Object> graph;
	private RepresentativeElectionAlgorithm<Object> algorithm;

	public RepresentativeElectionNode(ReteContainer reteContainer,
                                      RepresentativeElectionAlgorithm.Factory algorithmFactory) {
		super(reteContainer);
		this.algorithmFactory = algorithmFactory;
		graph = new Graph<>();
		algorithm = algorithmFactory.create(graph);
		algorithm.setObserver(this);
		reteContainer.registerClearable(this);
	}

	@Override
	public void networkStructureChanged() {
		if (reteContainer.isTimelyEvaluation() && reteContainer.getCommunicationTracker().isInRecursiveGroup(this)) {
			throw new IllegalStateException(this + " cannot be used in recursive differential dataflow evaluation!");
		}
		super.networkStructureChanged();
	}

	@Override
	public void reinitializeWith(Collection<Tuple> tuples) {
		algorithm.dispose();
		graph = new Graph<>();
		for (var tuple : tuples) {
			insertEdge(tuple.get(0), tuple.get(1));
		}
		algorithm = algorithmFactory.create(graph);
		algorithm.setObserver(this);
	}

	@Override
	public void tupleChanged(Object source, Object representative, Direction direction) {
		var tuple = Tuples.staticArityFlatTupleOf(source, representative);
		propagateUpdate(direction, tuple, Timestamp.ZERO);
	}

	@Override
	public void clear() {
		algorithm.dispose();
		graph = new Graph<>();
		algorithm = algorithmFactory.create(graph);
	}

	@Override
	public void update(Direction direction, Tuple updateElement, Timestamp timestamp) {
		var source = updateElement.get(0);
		var target = updateElement.get(1);
		switch (direction) {
		case INSERT -> insertEdge(source, target);
		case DELETE -> deleteEdge(source, target);
		default -> throw new IllegalArgumentException("Unknown direction: " + direction);
		}
	}

	private void insertEdge(Object source, Object target) {
		graph.insertNode(source);
		graph.insertNode(target);
		graph.insertEdge(source, target);
	}

	private void deleteEdge(Object source, Object target) {
		graph.deleteEdgeIfExists(source, target);
		if (isIsolated(source)) {
			graph.deleteNode(source);
		}
		if (!source.equals(target) && isIsolated(target)) {
			graph.deleteNode(target);
		}
	}

	private boolean isIsolated(Object node) {
		return graph.getTargetNodes(node).isEmpty() && graph.getSourceNodes(node).isEmpty();
	}

	@Override
	public void pullInto(Collection<Tuple> collector, boolean flush) {
		for (var entry : algorithm.getComponents().entrySet()) {
			var representative = entry.getKey();
			for (var node : entry.getValue()) {
				collector.add(Tuples.staticArityFlatTupleOf(node, representative));
			}
		}
	}

	@Override
	public void pullIntoWithTimeline(Map<Tuple, Timeline<Timestamp>> collector, boolean flush) {
		// Use all zero timestamps because this node cannot be used in recursive groups anyway.
		for (var entry : algorithm.getComponents().entrySet()) {
			var representative = entry.getKey();
			for (var node : entry.getValue()) {
				collector.put(Tuples.staticArityFlatTupleOf(node, representative), Timestamp.INSERT_AT_ZERO_TIMELINE);
			}
		}
	}
}
