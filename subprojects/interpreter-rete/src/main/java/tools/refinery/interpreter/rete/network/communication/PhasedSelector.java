/*******************************************************************************
 * Copyright (c) 2010-2017, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication;

/**
 * A default message selector that can be used to associate phases to messages.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public enum PhasedSelector implements MessageSelector {

    /**
     * No special distinguishing feature
     */
    DEFAULT,

    /**
     * Inserts and delete-insert monotone change pairs
     */
    MONOTONE,

    /**
     * Deletes
     */
    ANTI_MONOTONE

}
