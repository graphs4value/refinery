/*******************************************************************************
 * Copyright (c) 2010-2017, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication;

import java.util.Collection;
import java.util.Map;

import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;

/**
 * A communication group represents a set of nodes in the communication graph that form a strongly connected component.
 *
 * @author Tamas Szabo
 * @since 1.6
 */
public abstract class CommunicationGroup implements Comparable<CommunicationGroup> {

    public static final String UNSUPPORTED_MESSAGE_KIND = "Unsupported message kind ";

    /**
     * Marker for the {@link CommunicationTracker}
     */
    public boolean isEnqueued = false;

    protected final Node representative;

    /**
     * May be changed during bumping in {@link CommunicationTracker.registerDependency}
     */
    protected int identifier;

    /**
     * @since 1.7
     */
    protected final CommunicationTracker tracker;

    /**
     * @since 1.7
     */
    public CommunicationGroup(final CommunicationTracker tracker, final Node representative, final int identifier) {
        this.tracker = tracker;
        this.representative = representative;
        this.identifier = identifier;
    }

    public abstract void deliverMessages();

    public Node getRepresentative() {
        return representative;
    }

    public abstract boolean isEmpty();

    /**
     * @since 2.0
     */
    public abstract void notifyLostAllMessages(final Mailbox mailbox, final MessageSelector kind);

    /**
     * @since 2.0
     */
    public abstract void notifyHasMessage(final Mailbox mailbox, final MessageSelector kind);

    public abstract Map<MessageSelector, Collection<Mailbox>> getMailboxes();

    public abstract boolean isRecursive();

    @Override
    public int hashCode() {
        return this.identifier;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.identifier + " - representative: " + this.representative
                + " - isEmpty: " + isEmpty();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        } else if (this == obj) {
            return true;
        } else {
            final CommunicationGroup that = (CommunicationGroup) obj;
            return this.identifier == that.identifier;
        }
    }

    @Override
    public int compareTo(final CommunicationGroup that) {
        return this.identifier - that.identifier;
    }

}
