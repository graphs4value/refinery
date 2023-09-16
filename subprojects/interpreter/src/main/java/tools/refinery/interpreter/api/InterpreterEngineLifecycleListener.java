/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api;


/**
 * Listener interface for getting notification on changes in an {@link InterpreterEngine}.
 *
 * You can use it to remove any other listeners that you attached to matchers or the engine,
 * or to handle matchers that are initialized after you started using the engine.
 *
 * @author Abel Hegedus
 *
 */
public interface InterpreterEngineLifecycleListener {

    // -------------------------------------------------------------------------------
    // MATCHERS (methods notifying on changes in the matchers available in the engine)
    // -------------------------------------------------------------------------------

    /**
     * Called after a matcher is instantiated in the engine
     *
     * @param matcher the new matcher
     */
    void matcherInstantiated(InterpreterMatcher<? extends IPatternMatch> matcher);

    // -------------------------------------------------------------------------
    // HEALTH (methods notifying on changes that affect the health of the engine
    // -------------------------------------------------------------------------

    /**
     * Called after the engine has become tainted due to a fatal error
     */
    void engineBecameTainted(String message, Throwable t);

    /**
     * Called after the engine has been wiped
     */
    void engineWiped();

    /**
     * Called after the engine has been disposed
     */
    void engineDisposed();
}
