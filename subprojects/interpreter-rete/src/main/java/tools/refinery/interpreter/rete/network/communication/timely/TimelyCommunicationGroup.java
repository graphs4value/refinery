/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication.timely;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.rete.network.mailbox.timely.TimelyMailbox;
import tools.refinery.interpreter.rete.util.Options;

/**
 * A timely communication group implementation. {@link TimelyMailbox}es and {@link LazyFoldingNode}s are ordered in the
 * increasing order of timestamps.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public class TimelyCommunicationGroup extends CommunicationGroup {

    private final boolean isSingleton;
    private final TreeMap<Timestamp, Set<Mailbox>> mailboxQueue;
    // may be null - only used in the scattered case where we need to take care of mailboxes and resumables too
    private Comparator<Node> nodeComparator;
    private boolean currentlyDelivering;
    private Timestamp currentlyDeliveredTimestamp;

    public TimelyCommunicationGroup(final TimelyCommunicationTracker tracker, final Node representative,
            final int identifier, final boolean isSingleton) {
        super(tracker, representative, identifier);
        this.isSingleton = isSingleton;
        this.mailboxQueue = CollectionsFactory.createTreeMap();
        this.currentlyDelivering = false;
    }

    /**
     * Sets the {@link Comparator} to be used to order the {@link Mailbox}es at a given {@link Timestamp} in the mailbox
     * queue. Additionally, reorders already queued {@link Mailbox}es to reflect the new comparator. The comparator may
     * be null, in this case, no set ordering will be enforced among the {@link Mailbox}es.
     */
    public void setComparatorAndReorderMailboxes(final Comparator<Node> nodeComparator) {
        this.nodeComparator = nodeComparator;
        if (!this.mailboxQueue.isEmpty()) {
            final HashMap<Timestamp, Set<Mailbox>> queueCopy = new HashMap<Timestamp, Set<Mailbox>>(this.mailboxQueue);
            this.mailboxQueue.clear();
            for (final Entry<Timestamp, Set<Mailbox>> entry : queueCopy.entrySet()) {
                for (final Mailbox mailbox : entry.getValue()) {
                    this.notifyHasMessage(mailbox, entry.getKey());
                }
            }
        }
    }

    @Override
    public void deliverMessages() {
        this.currentlyDelivering = true;
        while (!this.mailboxQueue.isEmpty()) {
            // care must be taken here how we iterate over the mailboxes
            // it is not okay to loop over the mailboxes at once because a mailbox may disappear from the collection as
            // a result of delivering messages from another mailboxes under the same timestamp
            // because of this, it is crucial that we pick the mailboxes one by one
            final Entry<Timestamp, Set<Mailbox>> entry = this.mailboxQueue.firstEntry();
            final Timestamp timestamp = entry.getKey();
            final Set<Mailbox> mailboxes = entry.getValue();
            final Mailbox mailbox = mailboxes.iterator().next();
            mailboxes.remove(mailbox);
            if (mailboxes.isEmpty()) {
                this.mailboxQueue.pollFirstEntry();
            }
            assert mailbox instanceof TimelyMailbox;
            /* debug */ this.currentlyDeliveredTimestamp = timestamp;
            mailbox.deliverAll(timestamp);
            /* debug */ this.currentlyDeliveredTimestamp = null;
        }
        this.currentlyDelivering = false;
    }

    @Override
    public boolean isEmpty() {
        return this.mailboxQueue.isEmpty();
    }

    @Override
    public void notifyHasMessage(final Mailbox mailbox, MessageSelector kind) {
        if (kind instanceof Timestamp) {
            final Timestamp timestamp = (Timestamp) kind;
            if (Options.MONITOR_VIOLATION_OF_DIFFERENTIAL_DATAFLOW_TIMESTAMPS) {
                if (timestamp.compareTo(this.currentlyDeliveredTimestamp) < 0) {
                    final Logger logger = this.representative.getContainer().getNetwork().getEngine().getLogger();
                    logger.error(
                            "[INTERNAL ERROR] Violation of differential dataflow communication schema! The communication component with representative "
                                    + this.representative + " observed decreasing timestamp during message delivery!");
                }
            }
            final Set<Mailbox> mailboxes = this.mailboxQueue.computeIfAbsent(timestamp, k -> {
                if (this.nodeComparator == null) {
                    return CollectionsFactory.createSet();
                } else {
                    return new TreeSet<Mailbox>(new Comparator<Mailbox>() {
                        @Override
                        public int compare(final Mailbox left, final Mailbox right) {
                            return nodeComparator.compare(left.getReceiver(), right.getReceiver());
                        }
                    });
                }
            });
            mailboxes.add(mailbox);
            if (!this.isEnqueued && !this.currentlyDelivering) {
                this.tracker.activateUnenqueued(this);
            }
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_MESSAGE_KIND + kind);
        }
    }

    @Override
    public void notifyLostAllMessages(final Mailbox mailbox, final MessageSelector kind) {
        if (kind instanceof Timestamp) {
            final Timestamp timestamp = (Timestamp) kind;
            this.mailboxQueue.compute(timestamp, (k, v) -> {
                if (v == null) {
                    throw new IllegalStateException("No mailboxes registered at timestamp " + timestamp + "!");
                }
                if (!v.remove(mailbox)) {
                    throw new IllegalStateException(
                            "The mailbox " + mailbox + " was not registered at timestamp " + timestamp + "!");
                }
                if (v.isEmpty()) {
                    return null;
                } else {
                    return v;
                }
            });
            if (this.mailboxQueue.isEmpty()) {
                this.tracker.deactivate(this);
            }
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_MESSAGE_KIND + kind);
        }
    }

    @Override
    public Map<MessageSelector, Collection<Mailbox>> getMailboxes() {
        return Collections.unmodifiableMap(this.mailboxQueue);
    }

    @Override
    public boolean isRecursive() {
        return !this.isSingleton;
    }

}
