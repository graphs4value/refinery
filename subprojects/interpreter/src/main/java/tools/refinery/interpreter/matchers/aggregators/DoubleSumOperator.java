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
 * Incrementally computes the sum of java.lang.Double values
 * @author Gabor Bergmann
 * @since 1.4
 */
public class DoubleSumOperator extends AbstractMemorylessAggregationOperator<Double, Double> {
    public static final DoubleSumOperator INSTANCE = new DoubleSumOperator();

    private DoubleSumOperator() {
        // Singleton, do not call.
    }

    @Override
    public String getShortDescription() {
        return "sum<Double> incrementally computes the sum of java.lang.Double values";
    }
    @Override
    public String getName() {
        return "sum<Double>";
    }

    @Override
    public Double createNeutral() {
        return 0d;
    }

    @Override
    public boolean isNeutral(Double result) {
        return createNeutral().equals(result);
    }

    @Override
    public Double update(Double oldResult, Double updateValue, boolean isInsertion) {
        return isInsertion ?
                oldResult + updateValue :
                oldResult - updateValue;
    }

    /**
     * @since 2.0
     */
    @Override
    public Double aggregateStream(Stream<Double> stream) {
        return stream.mapToDouble(Double::doubleValue).sum();
    }


}
