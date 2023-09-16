/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * Internal interface for the query backend to singal an update to a query result.
 * @author Bergmann Gabor
 * @since 0.9
 *
 */
public interface IUpdateable {

    /**
     * This callback method must be free of exceptions, even {@link RuntimeException}s (though not {@link Error}s).
     * @param updateElement the tuple that is changed
     * @param isInsertion true if the tuple appeared in the result set, false if disappeared from the result set
     */
    public void update(Tuple updateElement, boolean isInsertion);
}
