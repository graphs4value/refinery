/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.aggregators;

import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.psystem.aggregations.AbstractMemorylessAggregationOperator;

/**
 * Incrementally computes the sum of java.lang.Integer values
 * @author Gabor Bergmann
 * @since 1.4
 */
public class IntegerSumOperator extends AbstractMemorylessAggregationOperator<Integer, Integer> {
    public static final IntegerSumOperator INSTANCE = new IntegerSumOperator();

    private IntegerSumOperator() {
        // Singleton, do not call.
    }

    @Override
    public String getShortDescription() {
        return "sum<Integer> incrementally computes the sum of java.lang.Integer values";
    }
    @Override
    public String getName() {
        return "sum<Integer>";
    }

    @Override
    public Integer createNeutral() {
        return 0;
    }

    @Override
    public boolean isNeutral(Integer result) {
        return createNeutral().equals(result);
    }

    @Override
    public Integer update(Integer oldResult, Integer updateValue, boolean isInsertion) {
        return isInsertion ?
                oldResult + updateValue :
                oldResult - updateValue;
    }

    /**
     * @since 2.0
     */
    @Override
    public Integer aggregateStream(Stream<Integer> stream) {
        return stream.mapToInt(Integer::intValue).sum();
    }

}
