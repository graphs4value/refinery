/*******************************************************************************
 * Copyright (c) 2010-2016, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.aggregators;

import tools.refinery.interpreter.matchers.psystem.aggregations.AggregatorType;
import tools.refinery.interpreter.matchers.psystem.aggregations.BoundAggregator;
import tools.refinery.interpreter.matchers.psystem.aggregations.IAggregatorFactory;

/**
 * An aggregator to count the number of matches a pattern has. The return of the aggregator is an non-negative integer
 * number.
 *
 * @since 1.4
 *
 */
@AggregatorType(parameterTypes = {Void.class}, returnTypes = {Integer.class})
public final class count implements IAggregatorFactory {

    @Override
    public BoundAggregator getAggregatorLogic(Class<?> domainClass) {
        if (Void.class.equals(domainClass))
            return new BoundAggregator(null, Void.class, Integer.class);
        else throw new IllegalArgumentException();
    }


}
