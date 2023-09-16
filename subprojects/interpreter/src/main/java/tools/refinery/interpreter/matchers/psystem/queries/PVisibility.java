/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.queries;

/**
 * @author Zoltan Ujhelyi
 * @since 2.0
 *
 */
public enum PVisibility {

    /**
     * A public (default) visibility means a pattern can be called at any time.
     */
    PUBLIC,
    /**
     * A private query is not expected to be called directly, only by a different query matcher.
     */
    PRIVATE,
    /**
     * A query that is only used inside a single caller query and is not visible outside its container query. Such
     * patterns must also fulfill the following additional constraints:
     *
     * <ul>
     * <li>An embedded query must have only a single body.</li>
     * <li>An embedded query must not be recursice.</li>
     * </ul>
     */
    EMBEDDED

}
