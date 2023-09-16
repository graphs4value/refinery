/*******************************************************************************
 * Copyright (c) 2010-2018, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.aggregators;

import java.util.OptionalDouble;
import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;

/**
 * @author Zoltan Ujhelyi
 * @since 2.0
 */
public class LongAverageOperator implements IMultisetAggregationOperator<Long, AverageAccumulator<Long>, Double> {

    public static final LongAverageOperator INSTANCE = new LongAverageOperator();

    private LongAverageOperator() {
        // Singleton, do not call.
    }

    @Override
    public String getShortDescription() {
        return "avg<Integer> incrementally computes the average of java.lang.Integer values";
    }

    @Override
    public String getName() {
        return "avg<Integer>";
    }

    @Override
    public AverageAccumulator<Long> createNeutral() {
        return new AverageAccumulator<Long>(0l, 0l);
    }

    @Override
    public boolean isNeutral(AverageAccumulator<Long> result) {
        return result.count == 0l;
    }

    @Override
    public AverageAccumulator<Long> update(AverageAccumulator<Long> oldResult, Long updateValue,
            boolean isInsertion) {
        if (isInsertion) {
            oldResult.value += updateValue;
            oldResult.count++;
        } else {
            oldResult.value -= updateValue;
            oldResult.count--;
        }
        return oldResult;
    }

    @Override
    public Double getAggregate(AverageAccumulator<Long> result) {
        return (result.count == 0)
                ? null
                : ((double)result.value)/result.count;
    }

    @Override
    public Double aggregateStream(Stream<Long> stream) {
        final OptionalDouble averageOpt = stream.mapToLong(Long::longValue).average();
        return averageOpt.isPresent() ? averageOpt.getAsDouble() : null;
    }

    /**
     * @since 2.4
     */
    @Override
    public AverageAccumulator<Long> clone(AverageAccumulator<Long> original) {
        return new AverageAccumulator<Long>(original.value, original.count);
    }

}
