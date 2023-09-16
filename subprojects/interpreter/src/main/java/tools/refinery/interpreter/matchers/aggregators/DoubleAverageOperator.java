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
public class DoubleAverageOperator implements IMultisetAggregationOperator<Double, AverageAccumulator<Double>, Double> {

    public static final DoubleAverageOperator INSTANCE = new DoubleAverageOperator();

    private DoubleAverageOperator() {
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
    public AverageAccumulator<Double> createNeutral() {
        return new AverageAccumulator<Double>(0d, 0l);
    }

    @Override
    public boolean isNeutral(AverageAccumulator<Double> result) {
        return result.count == 0l;
    }

    @Override
    public AverageAccumulator<Double> update(AverageAccumulator<Double> oldResult, Double updateValue,
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
    public Double getAggregate(AverageAccumulator<Double> result) {
        return (result.count == 0)
                ? null
                : result.value/result.count;
    }

    @Override
    public Double aggregateStream(Stream<Double> stream) {
        final OptionalDouble averageOpt = stream.mapToDouble(Double::doubleValue).average();
        return averageOpt.isPresent() ? averageOpt.getAsDouble() : null;
    }

    /**
     * @since 2.4
     */
    @Override
    public AverageAccumulator<Double> clone(AverageAccumulator<Double> original) {
        return new AverageAccumulator<Double>(original.value, original.count);
    }

}
