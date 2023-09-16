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
 * Listener interface to get notifications when a new managed engine is initialized.
 *
 * @author Abel Hegedus
 *
 */
public interface InterpreterEngineInitializationListener {

    /**
     * Called when a managed engine is initialized in the EngineManager.
     *
     * @param engine the initialized engine
     */
    void engineInitialized(AdvancedInterpreterEngine engine);

}
