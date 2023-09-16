/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.internal.apiimpl;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.api.InterpreterEngine;
import tools.refinery.interpreter.api.scope.IEngineContext;
import tools.refinery.interpreter.api.scope.IIndexingErrorListener;

/**
 * Internal interface for a Scope to reveal model contents to the engine.
 *
 * @author Bergmann Gabor
 *
 */
public abstract class EngineContextFactory {
    protected abstract IEngineContext createEngineContext(InterpreterEngine engine, IIndexingErrorListener errorListener, Logger logger);
}
