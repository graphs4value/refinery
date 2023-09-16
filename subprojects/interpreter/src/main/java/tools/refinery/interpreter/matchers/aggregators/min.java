/*******************************************************************************
 * Copyright (c) 2010-2016, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.aggregators;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;

import tools.refinery.interpreter.matchers.psystem.aggregations.AggregatorType;
import tools.refinery.interpreter.matchers.psystem.aggregations.BoundAggregator;
import tools.refinery.interpreter.matchers.psystem.aggregations.IAggregatorFactory;

/**
 * This aggregator calculates the minimum value of a selected aggregate parameter of a called pattern. The aggregate
 * parameter is selected with the '#' symbol; the aggregate parameter must not be used outside the aggregator call. The
 * other parameters of the call might be bound or unbound; bound parameters limit the matches to consider for the
 * minimum calculation.
 *
 * @since 1.4
 *
 */
@AggregatorType(
        // TODO T extends Comparable?
        parameterTypes = {BigDecimal.class, BigInteger.class, Boolean.class, Byte.class, Calendar.class, Character.class,
                Date.class, Double.class, Enum.class, Float.class, Integer.class, Long.class, Short.class, String.class},
        returnTypes = {BigDecimal.class, BigInteger.class, Boolean.class, Byte.class, Calendar.class, Character.class,
                Date.class, Double.class, Enum.class, Float.class, Integer.class, Long.class, Short.class, String.class})
public final class min implements IAggregatorFactory {

    @Override
    public BoundAggregator getAggregatorLogic(Class<?> domainClass) {
        if (Comparable.class.isAssignableFrom(domainClass))
            return new BoundAggregator(ExtremumOperator.getMin(), domainClass, domainClass);
        else throw new IllegalArgumentException();
    }

}
