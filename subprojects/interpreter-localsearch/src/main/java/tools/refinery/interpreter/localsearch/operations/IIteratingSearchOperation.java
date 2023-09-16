/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations;

import tools.refinery.interpreter.matchers.context.IInputKey;

/**
 * Denotes a {@link ISearchOperation} which involves iterating over an instances of an {@link IInputKey}
 *
 * @author Grill Balázs
 * @since 1.4
 *
 */
public interface IIteratingSearchOperation extends ISearchOperation{

    /**
     * Get the {@link IInputKey} which instances this operation iterates upon.
     */
    public IInputKey getIteratedInputKey();

}
