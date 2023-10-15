/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication.timeless;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.rete.index.DualInputNode;
import tools.refinery.interpreter.rete.index.Indexer;
import tools.refinery.interpreter.rete.index.IndexerListener;
import tools.refinery.interpreter.rete.index.IterableIndexer;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.RederivableNode;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.rete.network.mailbox.timeless.BehaviorChangingMailbox;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Timeless implementation of the communication tracker.
 *
 * @author Tamas Szabo
 * @since 2.2
 */
public class TimelessCommunicationTracker extends CommunicationTracker {
	public TimelessCommunicationTracker(Logger logger) {
		super(logger);
	}

    @Override
    protected CommunicationGroup createGroup(Node representative, int index) {
        final boolean isSingleton = isSingleton(representative);
        final boolean isReceiver = representative instanceof Receiver;
        final boolean isPosetIndifferent = isReceiver
                && ((Receiver) representative).getMailbox() instanceof BehaviorChangingMailbox;
        final boolean isSingletonInDRedMode = isSingleton && (representative instanceof RederivableNode)
                && ((RederivableNode) representative).isInDRedMode();

        CommunicationGroup group = null;
        // we can only use a singleton group iff
        // (1) the SCC has one node AND
        // (2) either we have a poset-indifferent mailbox OR the node is not even a receiver AND
        // (3) the node does not run in DRed mode in a singleton group
        if (isSingleton && (isPosetIndifferent || !isReceiver) && !isSingletonInDRedMode) {
            group = new SingletonCommunicationGroup(this, representative, index);
        } else {
            group = new RecursiveCommunicationGroup(this, representative, index);
        }

        return group;
    }

    @Override
    protected void reconstructQueueContents(final Set<CommunicationGroup> oldActiveGroups) {
        for (final CommunicationGroup oldGroup : oldActiveGroups) {
            for (final Entry<MessageSelector, Collection<Mailbox>> entry : oldGroup.getMailboxes().entrySet()) {
                for (final Mailbox mailbox : entry.getValue()) {
                    final CommunicationGroup newGroup = this.groupMap.get(mailbox.getReceiver());
                    newGroup.notifyHasMessage(mailbox, entry.getKey());
                }
            }

            if (oldGroup instanceof RecursiveCommunicationGroup) {
                for (final RederivableNode node : ((RecursiveCommunicationGroup) oldGroup).getRederivables()) {
                    final CommunicationGroup newGroup = this.groupMap.get(node);
                    if (!(newGroup instanceof RecursiveCommunicationGroup)) {
                        throw new IllegalStateException("The new group must also be recursive! " + newGroup);
                    }
                    ((RecursiveCommunicationGroup) newGroup).addRederivable(node);
                }
            }
        }
    }

    @Override
    public Mailbox proxifyMailbox(final Node requester, final Mailbox original) {
        return original;
    }

    @Override
    public IndexerListener proxifyIndexerListener(final Node requester, final IndexerListener original) {
        return original;
    }

    @Override
    protected void postProcessNode(final Node node) {
        if (node instanceof Receiver) {
            final Mailbox mailbox = ((Receiver) node).getMailbox();
            if (mailbox instanceof BehaviorChangingMailbox) {
                final CommunicationGroup group = this.groupMap.get(node);
                // a default mailbox must split its messages iff
                // (1) its receiver is in a recursive group and
                final boolean c1 = group.isRecursive();
                // (2) its receiver is at the SCC boundary of that group
                final boolean c2 = isAtSCCBoundary(node);
                // (3) its group consists of more than one node
                final boolean c3 = isSingleton(node);
                ((BehaviorChangingMailbox) mailbox).setSplitFlag(c1 && c2 && c3);
            }
        }
    }

    @Override
    protected void postProcessGroup(final CommunicationGroup group) {

    }

    /**
     * @since 2.0
     */
    private boolean isAtSCCBoundary(final Node node) {
        final CommunicationGroup ownGroup = this.groupMap.get(node);
        assert ownGroup != null;
        for (final Node source : this.dependencyGraph.getSourceNodes(node).distinctValues()) {
            final Set<Node> sourcesToCheck = new HashSet<Node>();
            sourcesToCheck.add(source);
            // DualInputNodes must be checked additionally because they do not use a mailbox directly.
            // It can happen that their indexers actually belong to other SCCs.
            if (source instanceof DualInputNode) {
                final DualInputNode dualInput = (DualInputNode) source;
                final IterableIndexer primarySlot = dualInput.getPrimarySlot();
                if (primarySlot != null) {
                    sourcesToCheck.add(primarySlot.getActiveNode());
                }
                final Indexer secondarySlot = dualInput.getSecondarySlot();
                if (secondarySlot != null) {
                    sourcesToCheck.add(secondarySlot.getActiveNode());
                }
            }
            for (final Node current : sourcesToCheck) {
                final CommunicationGroup otherGroup = this.groupMap.get(current);
                assert otherGroup != null;
                if (!ownGroup.equals(otherGroup)) {
                    return true;
                }
            }
        }
        return false;
    }

}
