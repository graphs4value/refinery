/*******************************************************************************
 * Copyright (c) 2004-2009 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.aggregation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.interpreter.rete.traceability.TraceInfo;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.rete.index.DefaultIndexerListener;
import tools.refinery.interpreter.rete.index.Indexer;
import tools.refinery.interpreter.rete.index.ProjectionIndexer;
import tools.refinery.interpreter.rete.index.StandardIndexer;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.StandardNode;
import tools.refinery.interpreter.rete.network.communication.Timestamp;

/**
 * A special node depending on a projection indexer to aggregate tuple groups with the same projection. Only propagates
 * the aggregates of non-empty groups. Use the outer indexers to circumvent.
 * <p>
 * This node cannot be used in recursive differential dataflow evaluation.
 *
 * @author Gabor Bergmann
 * @since 1.4
 */
public abstract class IndexerBasedAggregatorNode extends StandardNode implements IAggregatorNode {

    ProjectionIndexer projection;
    IndexerBasedAggregatorNode me;
    int sourceWidth;
    Map<Tuple, Object> mainAggregates;

    AggregatorOuterIndexer aggregatorOuterIndexer = null;
    AggregatorOuterIdentityIndexer[] aggregatorOuterIdentityIndexers = null;

    /**
     * MUST call initializeWith() afterwards!
     */
    public IndexerBasedAggregatorNode(ReteContainer reteContainer) {
        super(reteContainer);
        this.me = this;
        mainAggregates = CollectionsFactory.createMap();
    }

    @Override
    public void networkStructureChanged() {
        if (this.reteContainer.isTimelyEvaluation() && this.reteContainer.getCommunicationTracker().isInRecursiveGroup(this)) {
            throw new IllegalStateException(this.toString() + " cannot be used in recursive differential dataflow evaluation!");
        }
        super.networkStructureChanged();
    }

    /**
     * @param projection
     *            the projection indexer whose tuple groups should be aggregated
     */
    public void initializeWith(ProjectionIndexer projection) {
        this.projection = projection;
        this.sourceWidth = projection.getMask().indices.length;

        for (Tuple signature : projection.getSignatures()) {
            mainAggregates.put(signature, aggregateGroup(signature, projection.get(signature)));
        }
        projection.attachListener(new DefaultIndexerListener(this) {
            @Override
            public void notifyIndexerUpdate(Direction direction, Tuple updateElement, Tuple signature, boolean change, Timestamp timestamp) {
                aggregateUpdate(direction, updateElement, signature, change);
            }
        });
    }

    /**
     * Aggregates (reduces) a group of tuples. The group can be null.
     */
    public abstract Object aggregateGroup(Tuple signature, Collection<Tuple> group);


    /**
     * Aggregates (reduces) a group of tuples, having access to the previous aggregated value (before the update) and
     * the update definition. Defaults to aggregateGroup(). Override to increase performance.
     * @since 2.4
     */
    public Object aggregateGroupAfterUpdate(Tuple signature, Collection<Tuple> currentGroup, Object oldAggregate,
            Direction direction, Tuple updateElement, boolean change) {
        return aggregateGroup(signature, currentGroup);
    }

    protected Tuple aggregateAndPack(Tuple signature, Collection<Tuple> group) {
        return packResult(signature, aggregateGroup(signature, group));
    }

    @Override
    public Indexer getAggregatorOuterIndexer() {
        if (aggregatorOuterIndexer == null) {
            aggregatorOuterIndexer = new AggregatorOuterIndexer();
            this.getCommunicationTracker().registerDependency(this, aggregatorOuterIndexer);
            // reteContainer.connectAndSynchronize(this, aggregatorOuterIndexer);
        }
        return aggregatorOuterIndexer;
    }

    @Override
    public Indexer getAggregatorOuterIdentityIndexer(int resultPositionInSignature) {
        if (aggregatorOuterIdentityIndexers == null)
            aggregatorOuterIdentityIndexers = new AggregatorOuterIdentityIndexer[sourceWidth + 1];
        if (aggregatorOuterIdentityIndexers[resultPositionInSignature] == null) {
            aggregatorOuterIdentityIndexers[resultPositionInSignature] = new AggregatorOuterIdentityIndexer(
                    resultPositionInSignature);
            this.getCommunicationTracker().registerDependency(this, aggregatorOuterIdentityIndexers[resultPositionInSignature]);
            // reteContainer.connectAndSynchronize(this, aggregatorOuterIdentityIndexers[resultPositionInSignature]);
        }
        return aggregatorOuterIdentityIndexers[resultPositionInSignature];
    }

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
        for (final Entry<Tuple, Object> aggregateEntry : mainAggregates.entrySet()) {
            collector.add(packResult(aggregateEntry.getKey(), aggregateEntry.getValue()));
        }
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        // use all zero timestamps because this node cannot be used in recursive groups anyway
        for (final Entry<Tuple, Object> aggregateEntry : mainAggregates.entrySet()) {
            collector.put(packResult(aggregateEntry.getKey(), aggregateEntry.getValue()), Timestamp.INSERT_AT_ZERO_TIMELINE);
        }
    }

    protected Tuple packResult(Tuple signature, Object result) {
        return Tuples.staticArityLeftInheritanceTupleOf(signature, result);
    }

    /**
     * @since 2.4
     */
    protected void aggregateUpdate(Direction direction, Tuple updateElement, Tuple signature, boolean change) {
        Collection<Tuple> currentGroup = projection.get(signature);
        // these will be null if group is empty
        Object oldAggregate = mainAggregates.get(signature);
        Object safeOldAggregate = oldAggregate == null ? aggregateGroup(signature, null) : oldAggregate;
        boolean empty = currentGroup == null || currentGroup.isEmpty();
        Object newAggregate = empty ? null : aggregateGroupAfterUpdate(signature, currentGroup, safeOldAggregate/*
                                                                                                                 * non-null
                                                                                                                 */,
                direction, updateElement, change);
        if (!empty)
            mainAggregates.put(signature, newAggregate);
        else
            mainAggregates.remove(signature);
        Tuple oldTuple = packResult(signature, safeOldAggregate);
        Tuple newTuple = packResult(signature, newAggregate == null ? aggregateGroup(signature, null) : newAggregate);
        if (oldAggregate != null)
            propagateUpdate(Direction.DELETE, oldTuple, Timestamp.ZERO); // direct outputs lack non-empty groups
        if (newAggregate != null)
            propagateUpdate(Direction.INSERT, newTuple, Timestamp.ZERO); // direct outputs lack non-empty groups
        if (aggregatorOuterIndexer != null)
            aggregatorOuterIndexer.propagate(signature, oldTuple, newTuple);
        if (aggregatorOuterIdentityIndexers != null)
            for (AggregatorOuterIdentityIndexer aggregatorOuterIdentityIndexer : aggregatorOuterIdentityIndexers)
                if (aggregatorOuterIdentityIndexer != null)
                    aggregatorOuterIdentityIndexer.propagate(signature, oldTuple, newTuple);
    }

    private Object getAggregate(Tuple signature) {
        Object aggregate = mainAggregates.get(signature);
        return aggregate == null ? aggregateGroup(signature, null) : aggregate;
    }

    @Override
    public void assignTraceInfo(TraceInfo traceInfo) {
        super.assignTraceInfo(traceInfo);
        if (traceInfo.propagateToIndexerParent() && projection != null)
            projection.acceptPropagatedTraceInfo(traceInfo);
    }

    /**
     * A special non-iterable index that retrieves the aggregated, packed result (signature+aggregate) for the original
     * signature.
     *
     * @author Gabor Bergmann
     */
    class AggregatorOuterIndexer extends StandardIndexer {

        public AggregatorOuterIndexer() {
            super(me.reteContainer, TupleMask.omit(sourceWidth, sourceWidth + 1));
            this.parent = me;
        }

        @Override
        public Collection<Tuple> get(Tuple signature) {
            return Collections.singleton(packResult(signature, getAggregate(signature)));
        }

        public void propagate(Tuple signature, Tuple oldTuple, Tuple newTuple) {
            propagate(Direction.INSERT, newTuple, signature, false, Timestamp.ZERO);
            propagate(Direction.DELETE, oldTuple, signature, false, Timestamp.ZERO);
        }

        @Override
        public Node getActiveNode() {
            return projection.getActiveNode();
        }

    }

    /**
     * A special non-iterable index that checks a suspected aggregate value for a given signature. The signature for
     * this index is the original signature of the projection index, with the suspected result inserted at position
     * resultPositionInSignature.
     *
     * @author Gabor Bergmann
     */

    class AggregatorOuterIdentityIndexer extends StandardIndexer {
        int resultPositionInSignature;
        TupleMask pruneResult;
        TupleMask reorderMask;

        public AggregatorOuterIdentityIndexer(int resultPositionInSignature) {
            super(me.reteContainer, TupleMask.displace(sourceWidth, resultPositionInSignature, sourceWidth + 1));
            this.parent = me;
            // this.localAggregates = new HashMap<Tuple, Tuple>();
            this.resultPositionInSignature = resultPositionInSignature;
            this.pruneResult = TupleMask.omit(resultPositionInSignature, sourceWidth + 1);
            if (resultPositionInSignature == sourceWidth)
                this.reorderMask = null;
            else
                this.reorderMask = mask;
        }

        @Override
        public Collection<Tuple> get(Tuple signatureWithResult) {
            Tuple prunedSignature = pruneResult.transform(signatureWithResult);
            Object result = getAggregate(prunedSignature);
            if (signatureWithResult.get(resultPositionInSignature).equals(result))
                return Collections.singleton(signatureWithResult);
            else
                return null;
        }

        public void propagate(Tuple signature, Tuple oldTuple, Tuple newTuple) {
            propagate(Direction.INSERT, reorder(newTuple), signature, true, Timestamp.ZERO);
            propagate(Direction.DELETE, reorder(oldTuple), signature, true, Timestamp.ZERO);
        }

        private Tuple reorder(Tuple signatureWithResult) {
            Tuple transformed;
            if (reorderMask == null)
                transformed = signatureWithResult;
            else
                transformed = reorderMask.transform(signatureWithResult);
            return transformed;
        }

        @Override
        public Node getActiveNode() {
            return projection.getActiveNode();
        }
    }

}
