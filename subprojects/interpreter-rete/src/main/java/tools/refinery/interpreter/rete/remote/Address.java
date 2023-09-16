/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.remote;

import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.ReteContainer;

/**
 * Remote identifier of a node of type T.
 *
 * @author Gabor Bergmann
 *
 */
public class Address<T extends Node> {
    ReteContainer container;
    Long nodeId;
    /**
     * Feel free to leave null e.g. if node is in a separate JVM.
     */
    T nodeCache;

    /**
     * Address of local node (use only for containers in the same VM!)
     */
    public static <N extends Node> Address<N> of(N node) {
        return new Address<N>(node);
    }

    /**
     * General constructor.
     *
     * @param container
     * @param nodeId
     */
    public Address(ReteContainer container, Long nodeId) {
        super();
        this.container = container;
        this.nodeId = nodeId;
    }

    /**
     * Local-only constructor. (use only for containers in the same VM!)
     *
     * @param node
     *            the node to address
     */
    public Address(T node) {
        super();
        this.nodeCache = node;
        this.container = node.getContainer();
        this.nodeId = node.getNodeId();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((container == null) ? 0 : container.hashCode());
        result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Address<?>))
            return false;
        final Address<?> other = (Address<?>) obj;
        if (container == null) {
            if (other.container != null)
                return false;
        } else if (!container.equals(other.container))
            return false;
        if (nodeId == null) {
            if (other.nodeId != null)
                return false;
        } else if (!nodeId.equals(other.nodeId))
            return false;
        return true;
    }

    public ReteContainer getContainer() {
        return container;
    }

    public void setContainer(ReteContainer container) {
        this.container = container;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public T getNodeCache() {
        return nodeCache;
    }

    public void setNodeCache(T nodeCache) {
        this.nodeCache = nodeCache;
    }

    @Override
    public String toString() {
        if (nodeCache == null)
            return "A(" + nodeId + " @ " + container + ")";
        else
            return "A(" + nodeCache + ")";

    }

}
