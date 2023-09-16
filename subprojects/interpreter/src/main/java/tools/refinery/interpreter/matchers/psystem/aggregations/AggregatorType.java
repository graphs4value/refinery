/*******************************************************************************
 * Copyright (c) 2010-2016, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.aggregations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tools.refinery.interpreter.matchers.aggregators.count;

/**
 * The aggregator type annotation describes the type constraints for the selected aggregator. In version 1.4, two kinds of
 * aggregators are supported:
 *
 * <ol>
 * <li>An aggregator that does not consider any parameter value from the call ({@link count}), just calculates the
 * number of matches. This is represented by a single {@link Void} and a single corresponding return type.</li>
 * <li>An aggregator that considers a single parameter from the call, and executes some aggregate operations over it.
 * Such an aggregate operation can be defined over multiple types, where each possible parameter type has a corresponding return type declared.</li>
 * </ol>
 *
 * <strong>Important!</strong> The parameterTypes and returnTypes arrays must have
 * <ul>
 * <li>The same number of classes defined each.</li>
 * <li>Items are corresponded by index.</li>
 * <li>Items should represent data types</li>
 * </ul>
 *
 * @author Zoltan Ujhelyi
 * @since 1.4
 *
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface AggregatorType {

    Class<?>[] parameterTypes();

    Class<?>[] returnTypes();
}
