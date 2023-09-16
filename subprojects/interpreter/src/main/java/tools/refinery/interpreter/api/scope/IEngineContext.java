/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api.scope;

import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;

/**
 * The context of the engine is instantiated by the scope,
 * and provides information and services regarding the model the towards the engine.
 *
 * @author Bergmann Gabor
 *
 */
public interface IEngineContext {

    /**
     * Returns the base index.
     * @throws InterpreterRuntimeException if the base index cannot be accessed
     */
    IBaseIndex getBaseIndex();

    /**
     * Disposes this context object. Resources in the index may now be freed up.
     * No more methods should be called after this one.
     *
     * @throws IllegalStateException if there are any active listeners to the underlying index
     */
    void dispose();

    /**
     * Provides instance model information for pattern matching.
     *
     * <p> Implementors note: must be reentrant.
     * If called while index loading is already in progress, must return the single runtime context instance that will eventually index the model.
     * When the runtime query context is invoked in such a case, incomplete indexes are tolerable, but change notifications must be correctly provided as loading commences.
     *
     * @return a runtime context for pattern matching
     * @since 1.2
     * @throws InterpreterRuntimeException if the runtime context cannot be initialized
     */
    public IQueryRuntimeContext getQueryRuntimeContext();
}
