/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context;

import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * Listens for changes in the runtime context.
 * @author Bergmann Gabor
 *
 */
public interface IQueryRuntimeContextListener {

    /**
     * The given tuple was inserted into or removed from the input relation indicated by the given key.
     * @param key the key identifying the input relation that was updated
     * @param updateTuple the tuple that was inserted or removed
     * @param isInsertion true if it was an insertion, false otherwise.
     */
    public void update(IInputKey key, Tuple updateTuple, boolean isInsertion);
}
