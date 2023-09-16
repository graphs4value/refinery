/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem;

/**
 * An expression evaluator is used to execute arbitrary Java code during pattern matching. In order to include the
 * evaluation in the planning seemlessly it is expected from the evaluator implementors to report all used PVariables by
 * name.
 *
 * @author Zoltan Ujhelyi
 *
 */
public interface IExpressionEvaluator {

    /**
     * A textual description of the expression. Used only for debug purposes, but must not be null.
     */
    String getShortDescription();

    /**
     * All input parameter names should be reported correctly.
     */
    Iterable<String> getInputParameterNames();

    /**
     * The expression evaluator code
     *
     * @param provider
     *            the value provider is an engine-specific way of reading internal variable tuples to evaluate the
     *            expression with
     * @return the result of the expression: in case of predicate evaluation the return value must be true or false;
     *         otherwise the result can be an arbitrary object. No null values should be returned.
     * @throws Exception
     */
    Object evaluateExpression(IValueProvider provider) throws Exception;
}
