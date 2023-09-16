/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.aggregation.timely;

import java.util.Objects;
import java.util.TreeMap;

import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;

/**
 * First-only column aggregator with sequential aggregation architecture.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public class FirstOnlySequentialTimelyColumnAggregatorNode<Domain, Accumulator, AggregateResult>
        extends FirstOnlyTimelyColumnAggregatorNode<Domain, Accumulator, AggregateResult> {

    public FirstOnlySequentialTimelyColumnAggregatorNode(final ReteContainer reteContainer,
            final IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator,
            final TupleMask groupMask, final TupleMask columnMask) {
        super(reteContainer, operator, groupMask, columnMask);
    }

    /**
     * Accumulator gets modified only at the timestamp where the update happened. During the folding, accumulators are
     * never changed at higher timestamps. Aggregate results at higher timestamps may change due to the change at the
     * input timestamp. Uniqueness enforcement may require from aggregate results to jump up/down on demand during the
     * folding.
     */
    @Override
    public void update(final Direction direction, final Tuple update, final Timestamp timestamp) {
        final Tuple group = groupMask.transform(update);
        final Tuple value = columnMask.transform(update);
        @SuppressWarnings("unchecked")
        final Domain aggregand = (Domain) runtimeContext.unwrapElement(value.get(0));
        final boolean isInsertion = direction == Direction.INSERT;

        final AggregateResult previousResult = getResultRaw(group, timestamp, true);

        final Accumulator oldAccumulator = getAccumulator(group, timestamp);
        final AggregateResult oldResult = previousResult == null ? operator.getAggregate(oldAccumulator)
                : operator.combine(previousResult, oldAccumulator);

        final Accumulator newAccumulator = operator.update(oldAccumulator, aggregand, isInsertion);
        final AggregateResult newResult = previousResult == null ? operator.getAggregate(newAccumulator)
                : operator.combine(previousResult, newAccumulator);

        storeIfNotNeutral(group, newAccumulator, newResult, timestamp);

        propagateWithChecks(group, timestamp, previousResult, previousResult, oldResult, newResult);

        // fold up the state towards higher timestamps
        if (!Objects.equals(oldResult, newResult)) {
            AggregateResult previousOldResult = oldResult;
            AggregateResult previousNewResult = newResult;
            AggregateResult currentOldResult = null;
            AggregateResult currentNewResult = null;
            final TreeMap<Timestamp, CumulativeAggregate<Accumulator, AggregateResult>> groupEntries = this.memory
                    .get(group);

            Timestamp currentTimestamp = groupEntries == null ? null : groupEntries.higherKey(timestamp);

            while (currentTimestamp != null) {
                // they cannot be the same, otherwise we would not even be here
                assert !Objects.equals(previousOldResult, previousNewResult);

                final Accumulator accumulator = getAccumulator(group, currentTimestamp);
                currentOldResult = groupEntries.get(currentTimestamp).result;
                currentNewResult = operator.combine(previousNewResult, accumulator);

                // otherwise we would not be iterating over this timestamp
                assert !operator.isNeutral(accumulator);

                propagateWithChecks(group, currentTimestamp, previousOldResult, previousNewResult, currentOldResult,
                        currentNewResult);

                if (!Objects.equals(currentOldResult, currentNewResult)) {
                    storeIfNotNeutral(group, accumulator, currentNewResult, currentTimestamp);
                    previousOldResult = currentOldResult;
                    previousNewResult = currentNewResult;
                    currentTimestamp = groupEntries.higherKey(currentTimestamp);
                } else {
                    // we can stop the folding from here
                    break;
                }
            }
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
                return operator.createNeutral();
            } else {
                return entry.accumulator;
            }
        }
    }

}
