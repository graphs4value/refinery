/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.rete.network;

import tools.refinery.viatra.runtime.rete.network.communication.CommunicationGroup;

/**
 * @author Gabor Bergmann
 * @since 1.7
 */
public interface IGroupable {

    /**
     * @return the current group of the mailbox
     * @since 1.7
     */
    CommunicationGroup getCurrentGroup();

    /**
     * Sets the current group of the mailbox
     * @since 1.7
     */
    void setCurrentGroup(CommunicationGroup group);

}