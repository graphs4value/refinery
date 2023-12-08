/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.aggregation.timely;

import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.*;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.rete.aggregation.timely.FaithfulParallelTimelyColumnAggregatorNode.CumulativeAggregate;
import tools.refinery.interpreter.rete.aggregation.timely.FaithfulParallelTimelyColumnAggregatorNode.FoldingState;
import tools.refinery.interpreter.rete.aggregation.timely.FaithfulTimelyColumnAggregatorNode.MergeableFoldingState;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.communication.timely.ResumableNode;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Faithful column aggregator with parallel aggregation architecture.
 *
 * @author Tamas Szabo
 * @since 2.4
 *
 */
public class FaithfulParallelTimelyColumnAggregatorNode<Domain, Accumulator, AggregateResult> extends
        FaithfulTimelyColumnAggregatorNode<Domain, Accumulator, AggregateResult, CumulativeAggregate<Domain, Accumulator>, FoldingState<Domain>>
        implements ResumableNode {

    public FaithfulParallelTimelyColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final TupleMask groupMask, final TupleMask columnMask) {
        super(reteContainer, operator, groupMask, columnMask);
    }

    public FaithfulParallelTimelyColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final TupleMask groupMask, final int aggregatedColumn) {
        this(reteContainer, operator, groupMask, TupleMask.selectSingle(aggregatedColumn, groupMask.sourceWidth));
    }

    @Override
    protected Map<AggregateResult, Diff<Timestamp>> doFoldingStep(final Tuple group, final FoldingState<Domain> state,
            final Timestamp timestamp) {
        final CumulativeAggregate<Domain, Accumulator> aggregate = getAggregate(group, timestamp);
        if (state.delta.isEmpty()) {
            gcAggregates(aggregate, group, timestamp);
            return Collections.emptyMap();
        } else {
            final Map<AggregateResult, Diff<Timestamp>> diffMap = CollectionsFactory.createMap();
            final Timestamp nextTimestamp = this.aggregates.get(group).higherKey(timestamp);

            final AggregateResult currentOldResult = operator.getAggregate(aggregate.accumulator);

            for (final Entry<Domain, Integer> entry : state.delta.entriesWithMultiplicities()) {
                final boolean isInsertion = entry.getValue() > 0;
                final Domain aggregand = entry.getKey();
                for (int i = 0; i < Math.abs(entry.getValue()); i++) {
                    aggregate.accumulator = operator.update(aggregate.accumulator, aggregand, isInsertion);
                }
            }

            final AggregateResult currentNewResult = operator.getAggregate(aggregate.accumulator);

            if (!Objects.equals(currentOldResult, currentNewResult)) {
                // current old result disappears here
                appendDiff(currentOldResult, new Signed<>(Direction.DELETE, timestamp), diffMap);
                if (nextTimestamp != null) {
                    appendDiff(currentOldResult, new Signed<>(Direction.INSERT, nextTimestamp), diffMap);
                }

                // current new result appears here
                appendDiff(currentNewResult, new Signed<>(Direction.INSERT, timestamp), diffMap);
                if (nextTimestamp != null) {
                    appendDiff(currentNewResult, new Signed<>(Direction.DELETE, nextTimestamp), diffMap);
                }
            }

            gcAggregates(aggregate, group, timestamp);
            updateTimeline(group, diffMap);

            // prepare folding state for next timestamp
            if (nextTimestamp != null) {
                final FoldingState<Domain> newState = new FoldingState<>();
                newState.delta = state.delta;
                addFoldingState(group, newState, nextTimestamp);
            }

            return diffMap;
        }
    }

    @Override
    public void update(final Direction direction, final Tuple update, final Timestamp timestamp) {
        final Tuple group = groupMask.transform(update);
        final Tuple value = columnMask.transform(update);
        @SuppressWarnings("unchecked")
        final Domain aggregand = (Domain) runtimeContext.unwrapElement(value.get(0));
        final boolean isInsertion = direction == Direction.INSERT;

        final CumulativeAggregate<Domain, Accumulator> aggregate = getAggregate(group, timestamp);
        final FoldingState<Domain> state = new FoldingState<>();
        if (isInsertion) {
            aggregate.aggregands.addOne(aggregand);
            state.delta.addOne(aggregand);
        } else {
            aggregate.aggregands.removeOne(aggregand);
            state.delta.removeOne(aggregand);
        }

        addFoldingState(group, state, timestamp);
    }

    /**
     * Garbage collects the counter of the given group and timestamp if the bag of aggregands is empty.
     */
    @Override
    protected void gcAggregates(final CumulativeAggregate<Domain, Accumulator> aggregate, final Tuple group,
            final Timestamp timestamp) {
        if (aggregate.aggregands.isEmpty()) {
            final TreeMap<Timestamp, CumulativeAggregate<Domain, Accumulator>> groupAggregates = this.aggregates
                    .get(group);
            groupAggregates.remove(timestamp);
            if (groupAggregates.isEmpty()) {
                this.aggregates.remove(group);
            }
        }
    }

    /**
     * On-demand initializes and returns the aggregate for the given group and timestamp.
     */
    @Override
    protected CumulativeAggregate<Domain, Accumulator> getAggregate(final Tuple group, final Timestamp timestamp) {
        final TreeMap<Timestamp, CumulativeAggregate<Domain, Accumulator>> groupAggregates = this.aggregates
                .computeIfAbsent(group, k -> CollectionsFactory.createTreeMap());
        return groupAggregates.computeIfAbsent(timestamp, k -> {
            final CumulativeAggregate<Domain, Accumulator> aggregate = new CumulativeAggregate<>();
            final Entry<Timestamp, CumulativeAggregate<Domain, Accumulator>> lowerEntry = groupAggregates
                    .lowerEntry(timestamp);
            if (lowerEntry == null) {
                aggregate.accumulator = operator.createNeutral();
            } else {
                aggregate.accumulator = operator.clone(lowerEntry.getValue().accumulator);
            }
            return aggregate;
        });
    }

    @Override
    public AggregateResult getAggregateResult(final Tuple group) {
        final TreeMap<Timestamp, CumulativeAggregate<Domain, Accumulator>> groupAggregates = this.aggregates.get(group);
        if (groupAggregates != null) {
            final Entry<Timestamp, CumulativeAggregate<Domain, Accumulator>> lastEntry = groupAggregates.lastEntry();
            return operator.getAggregate(lastEntry.getValue().accumulator);
        } else {
            return NEUTRAL;
        }
    }

    protected static class CumulativeAggregate<Domain, Accumulator> {
        protected Accumulator accumulator;
        protected IDeltaBag<Domain> aggregands;

        protected CumulativeAggregate() {
            this.aggregands = CollectionsFactory.createDeltaBag();
        }

        @Override
        public String toString() {
            return "accumulator=" + accumulator + " aggregands=" + aggregands;
        }
    }

    protected static class FoldingState<Domain> implements MergeableFoldingState<FoldingState<Domain>> {
        protected IDeltaBag<Domain> delta;

        protected FoldingState() {
            this.delta = CollectionsFactory.createDeltaBag();
        }

        @Override
        public String toString() {
            return "delta=" + delta;
        }

        /**
         * The returned result will never be null, even if the resulting delta set is empty.
         */
        @Override
        public FoldingState<Domain> merge(final FoldingState<Domain> that) {
            Preconditions.checkArgument(that != null);
            // 'this' was the previously registered folding state
            // 'that' is the new folding state being pushed upwards
            final FoldingState<Domain> result = new FoldingState<>();
            this.delta.forEachEntryWithMultiplicities((d, m) -> result.delta.addSigned(d, m));
            that.delta.forEachEntryWithMultiplicities((d, m) -> result.delta.addSigned(d, m));
            return result;
        }

    }

}
