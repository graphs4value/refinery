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
import tools.refinery.interpreter.matchers.util.Signed;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.matchers.util.timeline.Timelines;
import tools.refinery.interpreter.rete.aggregation.AbstractColumnAggregatorNode;
import tools.refinery.interpreter.rete.aggregation.GroupedMap;
import tools.refinery.interpreter.rete.aggregation.timely.FaithfulTimelyColumnAggregatorNode.MergeableFoldingState;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.communication.timely.ResumableNode;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.rete.network.mailbox.timely.TimelyMailbox;

/**
 * Faithful timely implementation of the column aggregator node. Complete timelines (series of appearance &
 * disappearance) are maintained for tuples. <br>
 * <br>
 * Subclasses are responsible for implementing the aggregator architecture, and they must use the CumulativeAggregate
 * type parameter for that. <br>
 * <br>
 * This node supports recursive aggregation.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public abstract class FaithfulTimelyColumnAggregatorNode<Domain, Accumulator, AggregateResult, CumulativeAggregate, FoldingState extends MergeableFoldingState<FoldingState>>
        extends AbstractColumnAggregatorNode<Domain, Accumulator, AggregateResult> implements ResumableNode {

    protected final Map<Tuple, TreeMap<Timestamp, CumulativeAggregate>> aggregates;
    protected final Map<Tuple, Map<AggregateResult, Timeline<Timestamp>>> timelines;
    protected final TreeMap<Timestamp, Map<Tuple, FoldingState>> foldingState;
    protected CommunicationGroup communicationGroup;

    public FaithfulTimelyColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final TupleMask groupMask, final TupleMask columnMask) {
        super(reteContainer, operator, groupMask, columnMask);
        this.aggregates = CollectionsFactory.createMap();
        this.timelines = CollectionsFactory.createMap();
        this.foldingState = CollectionsFactory.createTreeMap();
        // mailbox MUST be instantiated after the fields are all set
        this.mailbox = instantiateMailbox();
    }

    @Override
    protected Mailbox instantiateMailbox() {
        return new TimelyMailbox(this, this.reteContainer);
    }

    @Override
    public void clear() {
        this.mailbox.clear();
        this.aggregates.clear();
        this.timelines.clear();
        this.children.clear();
        this.childMailboxes.clear();
        this.foldingState.clear();
    }

    /**
     * Registers the given folding state for the specified timestamp and tuple. If there is already a state stored, the
     * two states will be merged together.
     *
     *
     */
    protected void addFoldingState(final Tuple group, final FoldingState state, final Timestamp timestamp) {
        // assert !state.delta.isEmpty();
        final Map<Tuple, FoldingState> tupleMap = this.foldingState.computeIfAbsent(timestamp,
                k -> CollectionsFactory.createMap());
        tupleMap.compute(group, (k, v) -> {
            return v == null ? state : v.merge(state);
        });
    }

    @Override
    public Timestamp getResumableTimestamp() {
        if (this.foldingState.isEmpty()) {
            return null;
        } else {
            return this.foldingState.firstKey();
        }
    }

    @Override
    public void resumeAt(final Timestamp timestamp) {
        Timestamp current = this.getResumableTimestamp();
        if (current == null) {
            throw new IllegalStateException("There is nothing to fold!");
        } else if (current.compareTo(timestamp) != 0) {
            throw new IllegalStateException("Expected to continue folding at " + timestamp + "!");
        }

        final Map<Tuple, FoldingState> tupleMap = this.foldingState.remove(timestamp);
        for (final Entry<Tuple, FoldingState> groupEntry : tupleMap.entrySet()) {
            final Tuple group = groupEntry.getKey();
            final FoldingState value = groupEntry.getValue();
            final Map<AggregateResult, Diff<Timestamp>> diffMap = doFoldingStep(group, value, timestamp);
            for (final Entry<AggregateResult, Diff<Timestamp>> resultEntry : diffMap.entrySet()) {
                for (final Signed<Timestamp> signed : resultEntry.getValue()) {
                    propagate(signed.getDirection(), group, resultEntry.getKey(), signed.getPayload());
                }
            }
        }

        final Timestamp nextTimestamp = this.getResumableTimestamp();
        if (Objects.equals(timestamp, nextTimestamp)) {
            throw new IllegalStateException(
                    "Folding at " + timestamp + " produced more folding work at the same timestamp!");
        } else if (nextTimestamp != null) {
            this.communicationGroup.notifyHasMessage(this.mailbox, nextTimestamp);
        }
    }

    protected abstract Map<AggregateResult, Diff<Timestamp>> doFoldingStep(final Tuple group, final FoldingState state,
            final Timestamp timestamp);

    /**
     * Updates and garbage collects the timeline of the given tuple based on the given diffs.
     */
    protected void updateTimeline(final Tuple group, final Map<AggregateResult, Diff<Timestamp>> diffs) {
        if (!diffs.isEmpty()) {
            this.timelines.compute(group, (k, resultTimelines) -> {
                if (resultTimelines == null) {
                    resultTimelines = CollectionsFactory.createMap();
                }
                for (final Entry<AggregateResult, Diff<Timestamp>> entry : diffs.entrySet()) {
                    final AggregateResult result = entry.getKey();
                    resultTimelines.compute(result, (k2, oldResultTimeline) -> {
                        final Diff<Timestamp> currentResultDiffs = entry.getValue();
                        if (oldResultTimeline == null) {
                            oldResultTimeline = getInitialTimeline(result);
                        }
                        final Timeline<Timestamp> timeline = oldResultTimeline.mergeAdditive(currentResultDiffs);
                        if (timeline.isEmpty()) {
                            return null;
                        } else {
                            return timeline;
                        }
                    });
                }
                if (resultTimelines.isEmpty()) {
                    return null;
                } else {
                    return resultTimelines;
                }
            });
        }
    }

    /**
     * Garbage collects the counter of the given group and timestamp if the bag of aggregands is empty.
     */
    protected abstract void gcAggregates(final CumulativeAggregate aggregate, final Tuple group,
            final Timestamp timestamp);

    /**
     * On-demand initializes and returns the aggregate for the given group and timestamp.
     */
    protected abstract CumulativeAggregate getAggregate(final Tuple group, final Timestamp timestamp);

    protected static final Timeline<Timestamp> NEUTRAL_INITIAL_TIMELINE = Timestamp.INSERT_AT_ZERO_TIMELINE;
    protected static final Timeline<Timestamp> NON_NEUTRAL_INITIAL_TIMELINE = Timelines.createEmpty();

    protected Timeline<Timestamp> getInitialTimeline(final AggregateResult result) {
        if (NEUTRAL == result) {
            return NEUTRAL_INITIAL_TIMELINE;
        } else {
            return NON_NEUTRAL_INITIAL_TIMELINE;
        }
    }

    protected static <AggregateResult> void appendDiff(final AggregateResult result, final Signed<Timestamp> diff,
            final Map<AggregateResult, Diff<Timestamp>> diffs) {
        if (result != null) {
            diffs.compute(result, (k, timeLineDiff) -> {
                if (timeLineDiff == null) {
                    timeLineDiff = new Diff<>();
                }
                timeLineDiff.add(diff);
                return timeLineDiff;
            });
        }
    }

    @Override
    public Tuple getAggregateTuple(final Tuple group) {
        return tupleFromAggregateResult(group, getAggregateResult(group));
    }

    @Override
    public Map<AggregateResult, Timeline<Timestamp>> getAggregateResultTimeline(final Tuple group) {
        final Map<AggregateResult, Timeline<Timestamp>> resultTimelines = this.timelines.get(group);
        if (resultTimelines == null) {
            if (NEUTRAL == null) {
                return Collections.emptyMap();
            } else {
                return Collections.singletonMap(NEUTRAL, NEUTRAL_INITIAL_TIMELINE);
            }
        } else {
            return resultTimelines;
        }
    }

    @Override
    public Map<Tuple, Timeline<Timestamp>> getAggregateTupleTimeline(final Tuple group) {
        final Map<AggregateResult, Timeline<Timestamp>> resultTimelines = getAggregateResultTimeline(group);
        return new GroupedMap<AggregateResult, Timeline<Timestamp>>(group, resultTimelines, this.runtimeContext);
    }

    @Override
    public CommunicationGroup getCurrentGroup() {
        return communicationGroup;
    }

    @Override
    public void setCurrentGroup(final CommunicationGroup currentGroup) {
        this.communicationGroup = currentGroup;
    }

    protected interface MergeableFoldingState<T> {

        public abstract T merge(final T that);

    }

}
