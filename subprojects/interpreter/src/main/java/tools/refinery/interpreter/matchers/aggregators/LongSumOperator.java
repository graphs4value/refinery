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
 * Incrementally computes the sum of java.lang.Long values
 * @author Gabor Bergmann
 * @since 1.4
 */
public class LongSumOperator extends AbstractMemorylessAggregationOperator<Long, Long> {
    public static final LongSumOperator INSTANCE = new LongSumOperator();

    private LongSumOperator() {
        // Singleton, do not call.
    }

    @Override
    public String getShortDescription() {
        return "sum<Long> incrementally computes the sum of java.lang.Long values";
    }
    @Override
    public String getName() {
        return "sum<Long>";
    }

    @Override
    public Long createNeutral() {
        return 0L;
    }

    @Override
    public boolean isNeutral(Long result) {
        return createNeutral().equals(result);
    }

    @Override
    public Long update(Long oldResult, Long updateValue, boolean isInsertion) {
        return isInsertion ?
                oldResult + updateValue :
                oldResult - updateValue;
    }

    /**
     * @since 2.0
     */
    @Override
    public Long aggregateStream(Stream<Long> stream) {
        return stream.mapToLong(Long::longValue).sum();
    }

}
