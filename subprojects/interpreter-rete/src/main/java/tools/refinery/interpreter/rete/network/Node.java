/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.network;

import java.util.Set;

import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.traceability.TraceInfo;

/**
 * A node of a rete network, should be uniquely identified by network and nodeId. NodeId can be requested by registering
 * at the Network on construction.
 *
 * @author Gabor Bergmann
 */
public interface Node {
    /**
     * @return the network this node belongs to.
     */
    ReteContainer getContainer();

    /**
     * @return the identifier unique to this node within the network.
     */
    long getNodeId();

    /**
     * Assigns a descriptive tag to the node
     */
    void setTag(Object tag);

    /**
     * @return the tag of the node
     */
    Object getTag();

    /**
     * @return unmodifiable view of the list of traceability infos assigned to this node
     */
    Set<TraceInfo> getTraceInfos();

    /**
     * assigns new traceability info to this node
     */
    void assignTraceInfo(TraceInfo traceInfo);
    /**
     * accepts traceability info propagated to this node
     */
    void acceptPropagatedTraceInfo(TraceInfo traceInfo);

    default CommunicationTracker getCommunicationTracker() {
        return getContainer().getCommunicationTracker();
    }

}
