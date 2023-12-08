/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.aggregation.timely;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.IDeltaBag;
import tools.refinery.interpreter.matchers.util.Preconditions;
import tools.refinery.interpreter.matchers.util.Signed;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.rete.aggregation.timely.FaithfulSequentialTimelyColumnAggregatorNode.CumulativeAggregate;
import tools.refinery.interpreter.rete.aggregation.timely.FaithfulSequentialTimelyColumnAggregatorNode.FoldingState;
import tools.refinery.interpreter.rete.aggregation.timely.FaithfulTimelyColumnAggregatorNode.MergeableFoldingState;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.communication.timely.ResumableNode;

/**
 * Faithful column aggregator with sequential aggregation architecture.
 *
 * @author Tamas Szabo
 * @since 2.4
 *
 */
public class FaithfulSequentialTimelyColumnAggregatorNode<Domain, Accumulator, AggregateResult> extends
        FaithfulTimelyColumnAggregatorNode<Domain, Accumulator, AggregateResult, CumulativeAggregate<Domain, Accumulator, AggregateResult>, FoldingState<Domain, AggregateResult>>
        implements ResumableNode {

    protected boolean isRecursiveAggregation;

    public FaithfulSequentialTimelyColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final TupleMask groupMask, final TupleMask columnMask) {
        super(reteContainer, operator, groupMask, columnMask);
        this.isRecursiveAggregation = false;
    }

    @Override
    public void networkStructureChanged() {
        super.networkStructureChanged();
        this.isRecursiveAggregation = this.reteContainer.getCommunicationTracker().isInRecursiveGroup(this);
    }

    @Override
    protected Map<AggregateResult, Diff<Timestamp>> doFoldingStep(final Tuple group,
            final FoldingState<Domain, AggregateResult> state, final Timestamp timestamp) {
        final CumulativeAggregate<Domain, Accumulator, AggregateResult> aggregate = getAggregate(group, timestamp);
        if (state.delta.isEmpty() && Objects.equals(state.oldResult, state.newResult)) {
            gcAggregates(aggregate, group, timestamp);
            return Collections.emptyMap();
        } else {
            final Map<AggregateResult, Diff<Timestamp>> diffMap = CollectionsFactory.createMap();
            final Timestamp nextTimestamp = this.aggregates.get(group).higherKey(timestamp);

            final AggregateResult previousOldResult = state.oldResult;
            final AggregateResult previousNewResult = state.newResult;

            final AggregateResult currentOldResult = previousOldResult == null
                    ? operator.getAggregate(aggregate.positive)
                    : operator.combine(previousOldResult, aggregate.positive);

            for (final Entry<Domain, Integer> entry : state.delta.entriesWithMultiplicities()) {
                final boolean isInsertion = entry.getValue() > 0;
                final Domain aggregand = entry.getKey();
                if (isInsertion) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        if (isRecursiveAggregation) {
                            final boolean contains = aggregate.negative.containsNonZero(aggregand);
                            if (contains) {
                                aggregate.negative.addOne(aggregand);
                            } else {
                                aggregate.positive = operator.update(aggregate.positive, aggregand, true);
                            }
                        } else {
                            aggregate.positive = operator.update(aggregate.positive, aggregand, true);
                        }
                    }
                } else {
                    for (int i = 0; i < -entry.getValue(); i++) {
                        if (isRecursiveAggregation) {
                            final boolean contains = operator.contains(aggregand, aggregate.positive);
                            if (contains) {
                                aggregate.positive = operator.update(aggregate.positive, aggregand, false);
                            } else {
                                aggregate.negative.removeOne(aggregand);
                            }
                        } else {
                            aggregate.positive = operator.update(aggregate.positive, aggregand, false);
                        }
                    }
                }
            }

            final AggregateResult currentNewResult = previousNewResult == null
                    ? operator.getAggregate(aggregate.positive)
                    : operator.combine(previousNewResult, aggregate.positive);

            aggregate.cachedResult = currentNewResult;

            final boolean sameResult = Objects.equals(currentOldResult, currentNewResult);
            if (!sameResult) {
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
            if (nextTimestamp != null && !sameResult) {
                final FoldingState<Domain, AggregateResult> newState = new FoldingState<>();
                // DO NOT push forward the delta in the folding state!!! that one only affects the input timestamp
                newState.oldResult = currentOldResult;
                newState.newResult = currentNewResult;
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

        final AggregateResult previousResult = getResultRaw(group, timestamp, true);
        final FoldingState<Domain, AggregateResult> state = new FoldingState<Domain, AggregateResult>();
        if (isInsertion) {
            state.delta.addOne(aggregand);
        } else {
            state.delta.removeOne(aggregand);
        }
        state.oldResult = previousResult;
        state.newResult = previousResult;

        // it is acceptable if both oldResult and newResult are null at this point
        // in that case we did not have a previous entry at a lower timestamp

        addFoldingState(group, state, timestamp);
    }

    protected AggregateResult getResultRaw(final Tuple group, final Timestamp timestamp, final boolean lower) {
        final TreeMap<Timestamp, CumulativeAggregate<Domain, Accumulator, AggregateResult>> entryMap = this.aggregates
                .get(group);
        if (entryMap == null) {
            return null;
        } else {
            CumulativeAggregate<Domain, Accumulator, AggregateResult> aggregate = null;
            if (lower) {
                final Entry<Timestamp, CumulativeAggregate<Domain, Accumulator, AggregateResult>> lowerEntry = entryMap
                        .lowerEntry(timestamp);
                if (lowerEntry != null) {
                    aggregate = lowerEntry.getValue();
                }
            } else {
                aggregate = entryMap.get(timestamp);
            }
            if (aggregate == null) {
                return null;
            } else {
                return aggregate.cachedResult;
            }
        }
    }

    @Override
    protected void gcAggregates(final CumulativeAggregate<Domain, Accumulator, AggregateResult> aggregate,
            final Tuple group, final Timestamp timestamp) {
        if (operator.isNeutral(aggregate.positive) && aggregate.negative.isEmpty()) {
            final TreeMap<Timestamp, CumulativeAggregate<Domain, Accumulator, AggregateResult>> groupAggregates = this.aggregates
                    .get(group);
            groupAggregates.remove(timestamp);
            if (groupAggregates.isEmpty()) {
                this.aggregates.remove(group);
            }
        }
    }

    @Override
    protected CumulativeAggregate<Domain, Accumulator, AggregateResult> getAggregate(final Tuple group,
            final Timestamp timestamp) {
        final TreeMap<Timestamp, CumulativeAggregate<Domain, Accumulator, AggregateResult>> groupAggregates = this.aggregates
                .computeIfAbsent(group, k -> CollectionsFactory.createTreeMap());
        return groupAggregates.computeIfAbsent(timestamp, k -> {
            final CumulativeAggregate<Domain, Accumulator, AggregateResult> aggregate = new CumulativeAggregate<>();
            aggregate.positive = operator.createNeutral();
            return aggregate;
        });
    }

    @Override
    public AggregateResult getAggregateResult(final Tuple group) {
        final TreeMap<Timestamp, CumulativeAggregate<Domain, Accumulator, AggregateResult>> groupAggregates = this.aggregates
                .get(group);
        if (groupAggregates != null) {
            final Entry<Timestamp, CumulativeAggregate<Domain, Accumulator, AggregateResult>> lastEntry = groupAggregates
                    .lastEntry();
            return lastEntry.getValue().cachedResult;
        } else {
            return NEUTRAL;
        }
    }

    protected static class CumulativeAggregate<Domain, Accumulator, AggregateResult> {
        protected Accumulator positive;
        protected IDeltaBag<Domain> negative;
        protected AggregateResult cachedResult;

        protected CumulativeAggregate() {
            this.negative = CollectionsFactory.createDeltaBag();
        }

        @Override
        public String toString() {
            return "positive=" + positive + " negative=" + negative + " cachedResult=" + cachedResult;
        }
    }

    protected static class FoldingState<Domain, AggregateResult>
            implements MergeableFoldingState<FoldingState<Domain, AggregateResult>> {
        protected IDeltaBag<Domain> delta;
        protected AggregateResult oldResult;
        protected AggregateResult newResult;

        protected FoldingState() {
            this.delta = CollectionsFactory.createDeltaBag();
        }

        @Override
        public String toString() {
            return "delta=" + delta + " oldResult=" + oldResult + " newResult=" + newResult;
        }

        /**
         * The returned result will never be null, even if the resulting delta set is empty.
         */
        @Override
        public FoldingState<Domain, AggregateResult> merge(final FoldingState<Domain, AggregateResult> that) {
            Preconditions.checkArgument(that != null);
            // 'this' was the previously registered folding state
            // 'that' is the new folding state being pushed upwards
            final FoldingState<Domain, AggregateResult> result = new FoldingState<Domain, AggregateResult>();
            this.delta.forEachEntryWithMultiplicities((d, m) -> result.delta.addSigned(d, m));
            that.delta.forEachEntryWithMultiplicities((d, m) -> result.delta.addSigned(d, m));
            result.oldResult = this.oldResult;
            result.newResult = that.newResult;
            return result;
        }

    }

}
