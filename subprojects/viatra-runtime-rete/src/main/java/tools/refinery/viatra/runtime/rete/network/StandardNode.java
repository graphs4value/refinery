/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.rete.network;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;
import tools.refinery.viatra.runtime.matchers.util.Direction;
import tools.refinery.viatra.runtime.rete.index.GenericProjectionIndexer;
import tools.refinery.viatra.runtime.rete.index.ProjectionIndexer;
import tools.refinery.viatra.runtime.rete.network.communication.Timestamp;
import tools.refinery.viatra.runtime.rete.network.mailbox.Mailbox;
import tools.refinery.viatra.runtime.rete.traceability.TraceInfo;

/**
 * Base implementation for a supplier node.
 * 
 * @author Gabor Bergmann
 * 
 */
public abstract class StandardNode extends BaseNode implements Supplier, NetworkStructureChangeSensitiveNode {
    protected final List<Receiver> children = CollectionsFactory.createObserverList();
    /**
     * @since 2.2
     */
    protected final List<Mailbox> childMailboxes = CollectionsFactory.createObserverList();

    public StandardNode(final ReteContainer reteContainer) {
        super(reteContainer);
    }

    /**
     * @since 2.4
     */
    protected void propagateUpdate(final Direction direction, final Tuple updateElement, final Timestamp timestamp) {
        for (final Mailbox childMailbox : childMailboxes) {
            childMailbox.postMessage(direction, updateElement, timestamp);
        }
    }

    @Override
    public void appendChild(final Receiver receiver) {
        children.add(receiver);
        childMailboxes.add(this.getCommunicationTracker().proxifyMailbox(this, receiver.getMailbox()));
    }

    @Override
    public void removeChild(final Receiver receiver) {
        children.remove(receiver);
        Mailbox mailboxToRemove = null;
        for (final Mailbox mailbox : childMailboxes) {
            if (mailbox.getReceiver() == receiver) {
                mailboxToRemove = mailbox;
                break;
            }
        }
        assert mailboxToRemove != null;
        childMailboxes.remove(mailboxToRemove);
    }

    @Override
    public void networkStructureChanged() {
        childMailboxes.clear();
        for (final Receiver receiver : children) {
            childMailboxes.add(this.getCommunicationTracker().proxifyMailbox(this, receiver.getMailbox()));
        }
    }

    @Override
    public Collection<Receiver> getReceivers() {
        return children;
    }

    /**
     * @since 2.2
     */
    public Collection<Mailbox> getChildMailboxes() {
        return this.childMailboxes;
    }

    @Override
    public Set<Tuple> getPulledContents(final boolean flush) {
        final HashSet<Tuple> results = new HashSet<Tuple>();
        pullInto(results, flush);
        return results;
    }

    @Override
    public ProjectionIndexer constructIndex(final TupleMask mask, final TraceInfo... traces) {
        final GenericProjectionIndexer indexer = new GenericProjectionIndexer(reteContainer, mask);
        for (final TraceInfo traceInfo : traces) {
            indexer.assignTraceInfo(traceInfo);
        }
        reteContainer.connectAndSynchronize(this, indexer);
        return indexer;
    }

    /**
     * @since 1.6
     */
    protected void issueError(final String message, final Exception ex) {
        if (ex == null) {
            this.reteContainer.getNetwork().getEngine().getLogger().error(message);
        } else {
            this.reteContainer.getNetwork().getEngine().getLogger().error(message, ex);
        }
    }

}
