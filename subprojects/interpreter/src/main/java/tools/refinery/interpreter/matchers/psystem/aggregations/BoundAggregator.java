/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.aggregations;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.common.JavaTransitiveInstancesKey;

/**
 * Augments an aggregator operator with type bindings for the type of values being aggregated and the aggregate result.
 * <p> In case of <em>count</em>, the operator should be null.
 * @author Gabor Bergmann
 * @since 1.4
 */
public class BoundAggregator {
    private final IMultisetAggregationOperator<?, ?, ?> operator;
    private final Class<?> domainType;
    private final Class<?> aggregateResultType;

    public BoundAggregator(IMultisetAggregationOperator<?, ?, ?> operator,
            Class<?> domainType,
            Class<?> aggregateResultType) {
        super();
        this.operator = operator;
        this.domainType = domainType;
        this.aggregateResultType = aggregateResultType;
    }

    public IMultisetAggregationOperator<?, ?, ?> getOperator() {
        return operator;
    }

    public Class<?> getDomainType() {
        return domainType;
    }

    public Class<?> getAggregateResultType() {
        return aggregateResultType;
    }

    public IInputKey getDomainTypeAsInputKey() {
        return toJavaInputKey(domainType);
    }

    public IInputKey getAggregateResultTypeAsInputKey() {
        return toJavaInputKey(aggregateResultType);
    }

    private static IInputKey toJavaInputKey(Class<?> type) {
        if (type==null) {
            return null;
        } else {
            return new JavaTransitiveInstancesKey(type);
        }
    }
}
