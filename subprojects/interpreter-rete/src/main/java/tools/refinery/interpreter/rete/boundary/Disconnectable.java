/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.boundary;

/**
 * For objects that connect a RETE implementation to the underlying model.
 *
 * @author Gabor Bergmann
 *
 */
public interface Disconnectable {

    /**
     * Disconnects this rete engine component from the underlying model. Disconnecting enables the garbage collection
     * mechanisms to dispose of the rete network.
     */
    void disconnect();

}
