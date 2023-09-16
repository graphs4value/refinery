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
 * Helper interface to get values from a tuple of variables. All pattern matching engines are expected to implement this
 * to handle their internal structures.
 *
 * @author Zoltan Ujhelyi
 *
 */
public interface IValueProvider {

    /**
     * Returns the value of the selected variable
     * @param variableName
     * @return the value of the variable; never null
     * @throws IllegalArgumentException if the variable is not defined
     */
    Object getValue(String variableName);
}
