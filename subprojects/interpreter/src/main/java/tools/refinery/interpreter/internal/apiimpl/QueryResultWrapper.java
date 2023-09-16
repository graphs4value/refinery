/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.internal.apiimpl;

import tools.refinery.interpreter.matchers.backend.IMatcherCapability;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.api.InterpreterEngine;

/**
 * Internal class for wrapping a query result providing backend. It's only supported usage is by the
 * {@link InterpreterEngineImpl} class.
 * </p>
 *
 * <strong>Important note</strong>: this class must not introduce any public method, as it will be visible through
 * BaseMatcher as an API, although this class is not an API itself.
 *
 * @author Bergmann Gabor
 *
 */
public abstract class QueryResultWrapper {

    protected IQueryResultProvider backend;

    protected abstract void setBackend(InterpreterEngine engine, IQueryResultProvider resultProvider, IMatcherCapability capabilities);

}
