/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.aggregation.timely;

import java.util.Map.Entry;
import java.util.TreeMap;

import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;

/**
 * First-only column aggregator with parallel aggregation architecture.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public class FirstOnlyParallelTimelyColumnAggregatorNode<Domain, Accumulator, AggregateResult>
        extends FirstOnlyTimelyColumnAggregatorNode<Domain, Accumulator, AggregateResult> {

    public FirstOnlyParallelTimelyColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final TupleMask groupMask, final TupleMask columnMask) {
        super(reteContainer, operator, groupMask, columnMask);
    }

    /**
     * Accumulator gets modified at the input timestamp and at all higher timestamps. Folding cannot be interrupted if
     * the new aggregate result is the same as the old at an intermediate timestamp because aggregands need to be copied
     * over to all accumulators at the higher timestamps.
     */
    @Override
    public void update(final Direction direction, final Tuple update, final Timestamp timestamp) {
        final Tuple group = groupMask.transform(update);
        final Tuple value = columnMask.transform(update);
        @SuppressWarnings("unchecked")
        final Domain aggregand = (Domain) runtimeContext.unwrapElement(value.get(0));
        final boolean isInsertion = direction == Direction.INSERT;

        final AggregateResult previousResult = getResultRaw(group, timestamp, true);

        Accumulator oldAccumulator = getAccumulator(group, timestamp);
        AggregateResult oldResult = operator.getAggregate(oldAccumulator);

        Accumulator newAccumulator = operator.update(oldAccumulator, aggregand, isInsertion);
        AggregateResult newResult = operator.getAggregate(newAccumulator);

        storeIfNotNeutral(group, newAccumulator, newResult, timestamp);

        propagateWithChecks(group, timestamp, previousResult, previousResult, oldResult, newResult);

        AggregateResult previousOldResult = oldResult;
        AggregateResult previousNewResult = newResult;
        final TreeMap<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> groupEntries = this.memory
                .get(group);

        Timestamp currentTimestamp = groupEntries == null ? null : groupEntries.higherKey(timestamp);

        while (currentTimestamp != null) {
            final CumulativeAggregate<Accumulator, AggregateResult> groupEntry = groupEntries.get(currentTimestamp);
            oldResult = groupEntry.result;
            oldAccumulator = groupEntry.accumulator;
            newAccumulator = operator.update(oldAccumulator, aggregand, isInsertion);
            newResult = operator.getAggregate(newAccumulator);

            storeIfNotNeutral(group, newAccumulator, newResult, currentTimestamp);

            propagateWithChecks(group, currentTimestamp, previousOldResult, previousNewResult, oldResult, newResult);

            previousOldResult = oldResult;
            previousNewResult = newResult;
            currentTimestamp = groupEntries.higherKey(currentTimestamp);
        }
    }

    @Override
    protected Accumulator getAccumulator(final Tuple group, final Timestamp timestamp) {
        final TreeMap<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> entryMap = this.memory.get(group);
        if (entryMap == null) {
            return operator.createNeutral();
        } else {
            final CumulativeAggregate<Accumulator, AggregateResult> entry = entryMap.get(timestamp);
            if (entry == null) {
                final Entry<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> lowerEntry = entryMap
                        .lowerEntry(timestamp);
                if (lowerEntry == null) {
                    return operator.createNeutral();
                } else {
                    return operator.clone(lowerEntry.getValue().accumulator);
                }
            } else {
                return entry.accumulator;
            }
        }
    }

}
