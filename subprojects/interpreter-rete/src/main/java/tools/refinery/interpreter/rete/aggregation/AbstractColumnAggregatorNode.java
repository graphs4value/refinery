/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
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
import java.util.Objects;

import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.Clearable;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.rete.index.Indexer;
import tools.refinery.interpreter.rete.index.StandardIndexer;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.single.SingleInputNode;

/**
 * Groups incoming tuples by the given mask, and aggregates values at a specific index in each group.
 * <p>
 * Direct children are not supported, use via outer join indexers instead.
 * <p>
 * There are both timeless and timely implementations.
 *
 * @author Tamas Szabo
 * @since 2.2
 *
 */
public abstract class AbstractColumnAggregatorNode<Domain, Accumulator, AggregateResult> extends SingleInputNode
        implements Clearable, IAggregatorNode {

    /**
     * @since 1.6
     */
    protected final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator;

    /**
     * @since 1.6
     */
    protected final TupleMask groupMask;

    /**
     * @since 1.6
     */
    protected final TupleMask columnMask;

    /**
     * @since 1.6
     */
    protected final int sourceWidth;

    /**
     * @since 1.6
     */
    protected final IQueryRuntimeContext runtimeContext;

    protected final AggregateResult NEUTRAL;

    protected AggregatorOuterIndexer aggregatorOuterIndexer;

    @SuppressWarnings("rawtypes")
    protected AbstractColumnAggregatorNode.AggregatorOuterIdentityIndexer[] aggregatorOuterIdentityIndexers;

    /**
     * Creates a new column aggregator node.
     *
     * @param reteContainer
     *            the RETE container of the node
     * @param operator
     *            the aggregation operator
     * @param deleteRederiveEvaluation
     *            true if the node should run in DRED mode, false otherwise
     * @param groupMask
     *            the mask that masks a tuple to obtain the key that we are grouping-by
     * @param columnMask
     *            the mask that masks a tuple to obtain the tuple element(s) that we are aggregating over
     * @param posetComparator
     *            the poset comparator for the column, if known, otherwise it can be null
     * @since 1.6
     */
    public AbstractColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final TupleMask groupMask, final TupleMask columnMask) {
        super(reteContainer);
        this.operator = operator;
        this.groupMask = groupMask;
        this.columnMask = columnMask;
        this.sourceWidth = groupMask.indices.length;
        this.runtimeContext = reteContainer.getNetwork().getEngine().getRuntimeContext();
        this.NEUTRAL = operator.getAggregate(operator.createNeutral());
        reteContainer.registerClearable(this);
    }

    /**
     * Creates a new column aggregator node.
     *
     * @param reteContainer
     *            the RETE container of the node
     * @param operator
     *            the aggregation operator
     * @param groupMask
     *            the mask that masks a tuple to obtain the key that we are grouping-by
     * @param aggregatedColumn
     *            the index of the column that the aggregator node is aggregating over
     */
    public AbstractColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final TupleMask groupMask, final int aggregatedColumn) {
        this(reteContainer, operator, groupMask, TupleMask.selectSingle(aggregatedColumn, groupMask.sourceWidth));
    }

    @Override
    public CommunicationTracker getCommunicationTracker() {
        return this.reteContainer.getCommunicationTracker();
    }

    @Override
    public void pullInto(Collection<Tuple> collector, boolean flush) {
        // DIRECT CHILDREN NOT SUPPORTED
        throw new UnsupportedOperationException();
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        // DIRECT CHILDREN NOT SUPPORTED
        throw new UnsupportedOperationException();
    }

    @Override
    public void appendChild(Receiver receiver) {
        // DIRECT CHILDREN NOT SUPPORTED
        throw new UnsupportedOperationException();
    }

    @Override
    public Indexer getAggregatorOuterIndexer() {
        if (aggregatorOuterIndexer == null) {
            aggregatorOuterIndexer = new AggregatorOuterIndexer();
            this.getCommunicationTracker().registerDependency(this, aggregatorOuterIndexer);
        }
        return aggregatorOuterIndexer;
    }

    @Override
    public Indexer getAggregatorOuterIdentityIndexer(final int resultPositionInSignature) {
        if (aggregatorOuterIdentityIndexers == null) {
            aggregatorOuterIdentityIndexers = new AbstractColumnAggregatorNode.AggregatorOuterIdentityIndexer[sourceWidth
                    + 1];
        }
        if (aggregatorOuterIdentityIndexers[resultPositionInSignature] == null) {
            aggregatorOuterIdentityIndexers[resultPositionInSignature] = new AggregatorOuterIdentityIndexer(
                    resultPositionInSignature);
            this.getCommunicationTracker().registerDependency(this,
                    aggregatorOuterIdentityIndexers[resultPositionInSignature]);
        }
        return aggregatorOuterIdentityIndexers[resultPositionInSignature];
    }

    /**
     * @since 2.4
     */
    public void propagateAggregateResultUpdate(final Tuple group, final AggregateResult oldValue,
            final AggregateResult newValue, final Timestamp timestamp) {
        if (!Objects.equals(oldValue, newValue)) {
            propagate(Direction.DELETE, group, oldValue, timestamp);
            propagate(Direction.INSERT, group, newValue, timestamp);
        }
    }

    /**
     * @since 2.4
     */
    @SuppressWarnings("unchecked")
    public void propagate(final Direction direction, final Tuple group, final AggregateResult value,
            final Timestamp timestamp) {
        final Tuple tuple = tupleFromAggregateResult(group, value);

        if (aggregatorOuterIndexer != null) {
            aggregatorOuterIndexer.propagate(direction, tuple, group, timestamp);
        }
        if (aggregatorOuterIdentityIndexers != null) {
            for (final AggregatorOuterIdentityIndexer aggregatorOuterIdentityIndexer : aggregatorOuterIdentityIndexers) {
                if (aggregatorOuterIdentityIndexer != null) {
                    aggregatorOuterIdentityIndexer.propagate(direction, tuple, group, timestamp);
                }
            }
        }
    }

    public abstract Tuple getAggregateTuple(final Tuple key);

    /**
     * @since 2.4
     */
    public abstract Map<Tuple, Timeline<Timestamp>> getAggregateTupleTimeline(final Tuple key);

    public abstract AggregateResult getAggregateResult(final Tuple key);

    /**
     * @since 2.4
     */
    public abstract Map<AggregateResult, Timeline<Timestamp>> getAggregateResultTimeline(final Tuple key);

    protected Tuple tupleFromAggregateResult(final Tuple groupTuple, final AggregateResult aggregateResult) {
        if (aggregateResult == null) {
            return null;
        } else {
            return Tuples.staticArityLeftInheritanceTupleOf(groupTuple, runtimeContext.wrapElement(aggregateResult));
        }
    }

    /**
     * A special non-iterable index that retrieves the aggregated, packed result (signature+aggregate) for the original
     * signature.
     *
     * @author Gabor Bergmann
     * @author Tamas Szabo
     *
     */
    protected class AggregatorOuterIndexer extends StandardIndexer {

        /**
         * @since 2.4
         */
        protected NetworkStructureChangeSensitiveLogic logic;

        public AggregatorOuterIndexer() {
            super(AbstractColumnAggregatorNode.this.reteContainer, TupleMask.omit(sourceWidth, sourceWidth + 1));
            this.parent = AbstractColumnAggregatorNode.this;
            this.logic = createLogic();
        }

        @Override
        public void networkStructureChanged() {
            super.networkStructureChanged();
            this.logic = createLogic();
        }

        @Override
        public Collection<Tuple> get(final Tuple signature) {
            return this.logic.get(signature);
        }

        @Override
        public Map<Tuple, Timeline<Timestamp>> getTimeline(final Tuple signature) {
            return this.logic.getTimeline(signature);
        }

        /**
         * @since 2.4
         */
        public void propagate(final Direction direction, final Tuple tuple, final Tuple group,
                final Timestamp timestamp) {
            if (tuple != null) {
                propagate(direction, tuple, group, true, timestamp);
            }
        }

        @Override
        public Node getActiveNode() {
            return AbstractColumnAggregatorNode.this;
        }

        /**
         * @since 2.4
         */
        protected NetworkStructureChangeSensitiveLogic createLogic() {
            if (this.reteContainer.isTimelyEvaluation()
                    && this.reteContainer.getCommunicationTracker().isInRecursiveGroup(this)) {
                return this.TIMELY;
            } else {
                return this.TIMELESS;
            }
        }

        private final NetworkStructureChangeSensitiveLogic TIMELESS = new NetworkStructureChangeSensitiveLogic() {

            @Override
            public Collection<Tuple> get(final Tuple signature) {
                final Tuple aggregateTuple = getAggregateTuple(signature);
                if (aggregateTuple == null) {
                    return null;
                } else {
                    return Collections.singleton(aggregateTuple);
                }
            }

            @Override
            public Map<Tuple, Timeline<Timestamp>> getTimeline(final Tuple signature) {
                throw new UnsupportedOperationException();
            }

        };

        private final NetworkStructureChangeSensitiveLogic TIMELY = new NetworkStructureChangeSensitiveLogic() {

            @Override
            public Collection<Tuple> get(final Tuple signatureWithResult) {
                return TIMELESS.get(signatureWithResult);
            }

            @Override
            public Map<Tuple, Timeline<Timestamp>> getTimeline(final Tuple signature) {
                final Map<Tuple, Timeline<Timestamp>> aggregateTuples = getAggregateTupleTimeline(signature);
                if (aggregateTuples.isEmpty()) {
                    return null;
                } else {
                    return aggregateTuples;
                }
            }

        };

    }

    /**
     * A special non-iterable index that checks a suspected aggregate value for a given signature. The signature for
     * this index is the original 'group by' masked tuple, with the suspected result inserted at position
     * resultPositionInSignature.
     *
     * @author Gabor Bergmann
     * @author Tamas Szabo
     *
     */
    protected class AggregatorOuterIdentityIndexer extends StandardIndexer {

        protected final int resultPositionInSignature;
        protected final TupleMask pruneResult;
        protected final TupleMask reorderMask;
        /**
         * @since 2.4
         */
        protected NetworkStructureChangeSensitiveLogic logic;

        public AggregatorOuterIdentityIndexer(final int resultPositionInSignature) {
            super(AbstractColumnAggregatorNode.this.reteContainer,
                    TupleMask.displace(sourceWidth, resultPositionInSignature, sourceWidth + 1));
            this.resultPositionInSignature = resultPositionInSignature;
            this.pruneResult = TupleMask.omit(resultPositionInSignature, sourceWidth + 1);
            if (resultPositionInSignature == sourceWidth) {
                this.reorderMask = null;
            } else {
                this.reorderMask = mask;
            }
            this.logic = createLogic();
        }

        @Override
        public void networkStructureChanged() {
            super.networkStructureChanged();
            this.logic = createLogic();
        }

        @Override
        public Collection<Tuple> get(final Tuple signatureWithResult) {
            return this.logic.get(signatureWithResult);
        }

        /**
         * @since 2.4
         */
        @Override
        public Map<Tuple, Timeline<Timestamp>> getTimeline(final Tuple signature) {
            return this.logic.getTimeline(signature);
        }

        /**
         * @since 2.4
         */
        public void propagate(final Direction direction, final Tuple tuple, final Tuple group,
                final Timestamp timestamp) {
            if (tuple != null) {
                propagate(direction, reorder(tuple), group, true, timestamp);
            }
        }

        private Tuple reorder(final Tuple signatureWithResult) {
            Tuple transformed;
            if (reorderMask == null) {
                transformed = signatureWithResult;
            } else {
                transformed = reorderMask.transform(signatureWithResult);
            }
            return transformed;
        }

        @Override
        public Node getActiveNode() {
            return this.parent;
        }

        /**
         * @since 2.4
         */
        protected NetworkStructureChangeSensitiveLogic createLogic() {
            if (this.reteContainer.isTimelyEvaluation()
                    && this.reteContainer.getCommunicationTracker().isInRecursiveGroup(this)) {
                return this.TIMELY;
            } else {
                return this.TIMELESS;
            }
        }

        private final NetworkStructureChangeSensitiveLogic TIMELESS = new NetworkStructureChangeSensitiveLogic() {

            @Override
            public Collection<Tuple> get(final Tuple signatureWithResult) {
                final Tuple prunedSignature = pruneResult.transform(signatureWithResult);
                final AggregateResult result = getAggregateResult(prunedSignature);
                if (result != null && Objects.equals(signatureWithResult.get(resultPositionInSignature), result)) {
                    return Collections.singleton(signatureWithResult);
                } else {
                    return null;
                }
            }

            @Override
            public Map<Tuple, Timeline<Timestamp>> getTimeline(final Tuple signature) {
                throw new UnsupportedOperationException();
            }

        };

        private final NetworkStructureChangeSensitiveLogic TIMELY = new NetworkStructureChangeSensitiveLogic() {

            @Override
            public Collection<Tuple> get(final Tuple signatureWithResult) {
                return TIMELESS.get(signatureWithResult);
            }

            @Override
            public Map<Tuple, Timeline<Timestamp>> getTimeline(final Tuple signatureWithResult) {
                final Tuple prunedSignature = pruneResult.transform(signatureWithResult);
                final Map<AggregateResult, Timeline<Timestamp>> result = getAggregateResultTimeline(prunedSignature);
                for (final Entry<AggregateResult, Timeline<Timestamp>> entry : result.entrySet()) {
                    if (Objects.equals(signatureWithResult.get(resultPositionInSignature), entry.getKey())) {
                        return Collections.singletonMap(signatureWithResult, entry.getValue());
                    }
                }
                return null;
            }

        };

    }

    /**
     * @since 2.4
     */
    protected static abstract class NetworkStructureChangeSensitiveLogic {

        public abstract Collection<Tuple> get(final Tuple signatureWithResult);

        public abstract Map<Tuple, Timeline<Timestamp>> getTimeline(final Tuple signature);

    }

}
