/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner.cost;

/**
 * Common interface for cost function implementation
 *
 * @author Grill Balázs
 * @since 1.4
 *
 */
public interface ICostFunction{

    public double apply(IConstraintEvaluationContext input);

}
