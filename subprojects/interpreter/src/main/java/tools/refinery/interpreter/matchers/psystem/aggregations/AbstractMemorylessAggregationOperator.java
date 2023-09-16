/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.aggregations;

/**
 *
 * An aggregation operator that does not store interim results beyond the final aggregated value.
 * @author Gabor Bergmann
 * @since 1.4
 */
public abstract class AbstractMemorylessAggregationOperator<Domain, AggregateResult>
    implements IMultisetAggregationOperator<Domain, AggregateResult, AggregateResult>
{

    @Override
    public AggregateResult getAggregate(AggregateResult result) {
        return result;
    }

    @Override
    public AggregateResult clone(AggregateResult original) {
        return original;
    }

}
