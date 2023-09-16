/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.queries;

/**
 * Values of this enum describe a constraint to the calling of patterns regarding its parameters.
 *
 * @author Grill Balázs
 * @since 1.4
 *
 */
public enum PParameterDirection {

    /**
     * Default value, no additional constraint is applied
     */
    INOUT,

    /**
     * The parameters marked with this constraints shall be set to a value before calling the pattern
     */
    IN,

    /**
     * The parameters marked with this constraints shall not be set to a value before calling the pattern
     */
    OUT

}
