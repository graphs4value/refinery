/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api.scope;

/**
 *
 * This interface contains callbacks for various internal errors from the {@link NavigationHelper base index}.
 *
 * @author Zoltan Ujhelyi
 * @since 0.9
 *
 */
public interface IIndexingErrorListener {

    void error(String description, Throwable t);
    void fatal(String description, Throwable t);
}
