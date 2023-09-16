/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.aggregation.timely;

import java.util.Collection;
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
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.matchers.util.timeline.Timelines;
import tools.refinery.interpreter.rete.aggregation.AbstractColumnAggregatorNode;
import tools.refinery.interpreter.rete.aggregation.GroupedMap;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.rete.network.mailbox.timely.TimelyMailbox;

/**
 * First-only timely implementation of the column aggregator node. Only timestamps of appearance are maintained for
 * tuples instead of complete timelines.
 * <br><br>
 * Subclasses are responsible for implementing the aggregator architecture, and they must make use of the inner class {@link CumulativeAggregate}.
 * <br><br>
 * This node supports recursive aggregation.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public abstract class FirstOnlyTimelyColumnAggregatorNode<Domain, Accumulator, AggregateResult>
        extends AbstractColumnAggregatorNode<Domain, Accumulator, AggregateResult> {

    protected final Map<Tuple, TreeMap<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>>> memory;

    public FirstOnlyTimelyColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final TupleMask groupMask, final TupleMask columnMask) {
        super(reteContainer, operator, groupMask, columnMask);
        this.memory = CollectionsFactory.createMap();
        // mailbox MUST be instantiated after the fields are all set
        this.mailbox = instantiateMailbox();
    }

    protected static class CumulativeAggregate<Accumulator, AggregateResult> {
        // the accumulator storing the aggregands
        protected Accumulator accumulator;
        // the aggregate result at the timestamp where this cumulative aggregate is stored
        protected AggregateResult result;

        private CumulativeAggregate(final Accumulator accumulator, final AggregateResult result) {
            this.accumulator = accumulator;
            this.result = result;
        }

    }

    public Collection<Tuple> getGroups() {
        return this.memory.keySet();
    }

    public AggregateResult getLastResult(final Tuple group) {
        final TreeMap<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> groupMap = this.memory.get(group);
        if (groupMap == null) {
            return null;
        } else {
            return groupMap.lastEntry().getValue().result;
        }
    }

    public Timestamp getLastTimestamp(final Tuple group) {
        final TreeMap<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> groupMap = this.memory.get(group);
        if (groupMap == null) {
            return null;
        } else {
            return groupMap.lastEntry().getKey();
        }
    }

    @Override
    protected Mailbox instantiateMailbox() {
        return new TimelyMailbox(this, this.reteContainer);
    }

    @Override
    public void clear() {
        this.mailbox.clear();
        this.memory.clear();
        this.children.clear();
        this.childMailboxes.clear();
    }

    protected void propagateWithChecks(final Tuple group, final Timestamp timestamp,
            final AggregateResult previousOldResult, final AggregateResult previousNewResult,
            final AggregateResult currentOldResult, final AggregateResult currentNewResult) {
        final boolean jumpDown = Objects.equals(previousNewResult, currentOldResult);
        final boolean jumpUp = Objects.equals(previousOldResult, currentNewResult);
        final boolean resultsDiffer = !Objects.equals(currentOldResult, currentNewResult);

        // uniqueness enforcement is happening here
        if ((resultsDiffer || jumpDown) && !Objects.equals(previousOldResult, currentOldResult)) {
            propagate(Direction.DELETE, group, currentOldResult, timestamp);
        }
        if ((resultsDiffer || jumpUp) && !Objects.equals(previousNewResult, currentNewResult)) {
            propagate(Direction.INSERT, group, currentNewResult, timestamp);
        }
    }

    /**
     * Returns the aggregation architecture-specific accumulator at the specified timestamp for the given group.
     */
    protected abstract Accumulator getAccumulator(final Tuple group, final Timestamp timestamp);

    protected AggregateResult getResultRaw(final Tuple group, final Timestamp timestamp, final boolean lower) {
        final TreeMap<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> entryMap = this.memory.get(group);
        if (entryMap == null) {
            return null;
        } else {
            CumulativeAggregate<Accumulator, AggregateResult> entry = null;
            if (lower) {
                final Entry<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> lowerEntry = entryMap
                        .lowerEntry(timestamp);
                if (lowerEntry != null) {
                    entry = lowerEntry.getValue();
                }
            } else {
                entry = entryMap.get(timestamp);
            }
            if (entry == null) {
                return null;
            } else {
                return entry.result;
            }
        }
    }

    protected AggregateResult getResult(final Tuple group, final Timestamp timestamp, final boolean lower) {
        final AggregateResult result = getResultRaw(group, timestamp, lower);
        if (result == null) {
            return NEUTRAL;
        } else {
            return result;
        }
    }

    protected AggregateResult getResult(final Tuple group, final Timestamp timestamp) {
        return getResult(group, timestamp, false);
    }

    protected void storeIfNotNeutral(final Tuple group, final Accumulator accumulator, final AggregateResult value,
            final Timestamp timestamp) {
        TreeMap<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> entryMap = this.memory.get(group);
        if (operator.isNeutral(accumulator)) {
            if (entryMap != null) {
                entryMap.remove(timestamp);
                if (entryMap.isEmpty()) {
                    this.memory.remove(group);
                }
            }
        } else {
            if (entryMap == null) {
                entryMap = CollectionsFactory.createTreeMap();
                this.memory.put(group, entryMap);
            }
            entryMap.put(timestamp, new CumulativeAggregate<>(accumulator, value));
        }
    }

    @Override
    public Tuple getAggregateTuple(final Tuple group) {
        return tupleFromAggregateResult(group, getResult(group, Timestamp.ZERO));
    }

    @Override
    public AggregateResult getAggregateResult(final Tuple group) {
        return getResult(group, Timestamp.ZERO);
    }

    @Override
    public Map<AggregateResult, Timeline<Timestamp>> getAggregateResultTimeline(final Tuple group) {
        final TreeMap<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> entryMap = this.memory.get(group);
        if (entryMap == null) {
            return Collections.emptyMap();
        } else {
            final Map<AggregateResult, Timeline<Timestamp>> result = CollectionsFactory.createMap();
            for (final Entry<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> entry : entryMap
                    .descendingMap().entrySet()) {
                result.put(entry.getValue().result, Timelines.createFrom(entry.getKey()));
            }
            return result;
        }
    }

    @Override
    public Map<Tuple, Timeline<Timestamp>> getAggregateTupleTimeline(final Tuple group) {
        final Map<AggregateResult, Timeline<Timestamp>> resultTimelines = getAggregateResultTimeline(group);
        return new GroupedMap<AggregateResult, Timeline<Timestamp>>(group, resultTimelines, this.runtimeContext);
    }

}
