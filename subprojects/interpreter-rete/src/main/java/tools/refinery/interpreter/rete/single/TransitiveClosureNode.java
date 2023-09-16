/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.single;

import tools.refinery.interpreter.rete.network.NetworkStructureChangeSensitiveNode;
import tools.refinery.interpreter.rete.network.ReinitializedNode;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.itc.alg.incscc.IncSCCAlg;
import tools.refinery.interpreter.rete.itc.alg.misc.Tuple;
import tools.refinery.interpreter.rete.itc.graphimpl.Graph;
import tools.refinery.interpreter.rete.itc.igraph.ITcDataSource;
import tools.refinery.interpreter.rete.itc.igraph.ITcObserver;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.Clearable;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

import java.util.Collection;
import java.util.Map;

/**
 * This class represents a transitive closure node in the Rete net.
 * <p>
 * This node must not be used in recursive {@link CommunicationGroup}s.
 *
 * @author Gabor Bergmann
 *
 */
public class TransitiveClosureNode extends SingleInputNode
        implements Clearable, ITcObserver<Object>, NetworkStructureChangeSensitiveNode, ReinitializedNode {

    private Graph<Object> graphDataSource;
    private ITcDataSource<Object> transitiveClosureAlgorithm;

    /**
     * Create a new transitive closure rete node.
     *
     * Client may optionally call {@link #reinitializeWith(Collection)} before using the node, instead of inserting the
     * initial set of tuples one by one.
     *
     * @param reteContainer
     *            the rete container of the node
     */
    public TransitiveClosureNode(ReteContainer reteContainer) {
        super(reteContainer);
        graphDataSource = new Graph<Object>();
        transitiveClosureAlgorithm = new IncSCCAlg<Object>(graphDataSource);
        transitiveClosureAlgorithm.attachObserver(this);
        reteContainer.registerClearable(this);
    }

    @Override
    public void networkStructureChanged() {
        if (this.reteContainer.isTimelyEvaluation() && this.reteContainer.getCommunicationTracker().isInRecursiveGroup(this)) {
            throw new IllegalStateException(this.toString() + " cannot be used in recursive differential dataflow evaluation!");
        }
        super.networkStructureChanged();
    }

    /**
     * Initializes the graph data source with the given collection of tuples.
     *
     * @param tuples
     *            the initial collection of tuples
     */
	@Override
    public void reinitializeWith(Collection<tools.refinery.interpreter.matchers.tuple.Tuple> tuples) {
        clear();

        for (tools.refinery.interpreter.matchers.tuple.Tuple t : tuples) {
            graphDataSource.insertNode(t.get(0));
            graphDataSource.insertNode(t.get(1));
            graphDataSource.insertEdge(t.get(0), t.get(1));
        }
        transitiveClosureAlgorithm.attachObserver(this);
    }

    @Override
    public void pullInto(final Collection<tools.refinery.interpreter.matchers.tuple.Tuple> collector, final boolean flush) {
        for (final Tuple<Object> tuple : ((IncSCCAlg<Object>) transitiveClosureAlgorithm).getTcRelation()) {
            collector.add(Tuples.staticArityFlatTupleOf(tuple.getSource(), tuple.getTarget()));
        }
    }

    @Override
    public void pullIntoWithTimeline(
            final Map<tools.refinery.interpreter.matchers.tuple.Tuple, Timeline<Timestamp>> collector,
            final boolean flush) {
        // use all zero timestamps because this node cannot be used in recursive groups anyway
        for (final Tuple<Object> tuple : ((IncSCCAlg<Object>) transitiveClosureAlgorithm).getTcRelation()) {
            collector.put(Tuples.staticArityFlatTupleOf(tuple.getSource(), tuple.getTarget()), Timestamp.INSERT_AT_ZERO_TIMELINE);
        }
    }

    @Override
    public void update(Direction direction, tools.refinery.interpreter.matchers.tuple.Tuple updateElement,
            Timestamp timestamp) {
        if (updateElement.getSize() == 2) {
            Object source = updateElement.get(0);
            Object target = updateElement.get(1);

            if (direction == Direction.INSERT) {
                graphDataSource.insertNode(source);
                graphDataSource.insertNode(target);
                graphDataSource.insertEdge(source, target);
            }
            if (direction == Direction.DELETE) {
                graphDataSource.deleteEdgeIfExists(source, target);

                if (((IncSCCAlg<Object>) transitiveClosureAlgorithm).isIsolated(source)) {
                    graphDataSource.deleteNode(source);
                }
                if (!source.equals(target) && ((IncSCCAlg<Object>) transitiveClosureAlgorithm).isIsolated(target)) {
                    graphDataSource.deleteNode(target);
                }
            }
        }
    }

    @Override
    public void clear() {
        transitiveClosureAlgorithm.dispose();
        graphDataSource = new Graph<Object>();
        transitiveClosureAlgorithm = new IncSCCAlg<Object>(graphDataSource);
    }

    @Override
    public void tupleInserted(Object source, Object target) {
        tools.refinery.interpreter.matchers.tuple.Tuple tuple = Tuples.staticArityFlatTupleOf(source, target);
        propagateUpdate(Direction.INSERT, tuple, Timestamp.ZERO);
    }

    @Override
    public void tupleDeleted(Object source, Object target) {
        tools.refinery.interpreter.matchers.tuple.Tuple tuple = Tuples.staticArityFlatTupleOf(source, target);
        propagateUpdate(Direction.DELETE, tuple, Timestamp.ZERO);
    }

}
