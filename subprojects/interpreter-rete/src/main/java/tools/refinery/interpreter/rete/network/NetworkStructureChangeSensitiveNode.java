/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network;

import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;

/**
 * {@link Node}s implementing this interface are sensitive to changes in the dependency graph maintained by the
 * {@link CommunicationTracker}. The {@link CommunicationTracker} notifies these nodes whenever the SCC of this node is
 * affected by changes to the dependency graph. Depending on whether this node is contained in a recursive group or not,
 * it may behave differently, and the {@link NetworkStructureChangeSensitiveNode#networkStructureChanged()} method can
 * be used to perform changes in behavior.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public interface NetworkStructureChangeSensitiveNode extends Node {

    /**
     * At the time of the invocation, the dependency graph has already been updated.
     */
    public void networkStructureChanged();

}
