/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations;

import tools.refinery.interpreter.localsearch.operations.util.CallInformation;

/**
 * Marker interface for pattern matcher call operations, such as positive and negative pattern calls or match aggregators.
 *
 * @author Zoltan Ujhelyi
 * @since 1.7
 */
public interface IPatternMatcherOperation {

    /**
     * Returns the precomputed call information associated with the current operation
     * @since 2.0
     */
    CallInformation getCallInformation();
}
