/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication.timeless;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.RederivableNode;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.communication.PhasedSelector;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;

/**
 * A communication group representing either a single node where the
 * node is a monotonicity aware one or a set of nodes that form an SCC.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public class RecursiveCommunicationGroup extends CommunicationGroup {

    private final Set<Mailbox> antiMonotoneMailboxes;
    private final Set<Mailbox> monotoneMailboxes;
    private final Set<Mailbox> defaultMailboxes;
    private final Set<RederivableNode> rederivables;
    private boolean currentlyDelivering;

    /**
     * @since 1.7
     */
    public RecursiveCommunicationGroup(final CommunicationTracker tracker, final Node representative, final int identifier) {
        super(tracker, representative, identifier);
        this.antiMonotoneMailboxes = CollectionsFactory.createSet();
        this.monotoneMailboxes = CollectionsFactory.createSet();
        this.defaultMailboxes = CollectionsFactory.createSet();
        this.rederivables = new LinkedHashSet<RederivableNode>();
        this.currentlyDelivering = false;
    }

    @Override
    public void deliverMessages() {
        this.currentlyDelivering = true;

        // ANTI-MONOTONE PHASE
        while (!this.antiMonotoneMailboxes.isEmpty() || !this.defaultMailboxes.isEmpty()) {
            while (!this.antiMonotoneMailboxes.isEmpty()) {
                final Mailbox mailbox = this.antiMonotoneMailboxes.iterator().next();
                this.antiMonotoneMailboxes.remove(mailbox);
                mailbox.deliverAll(PhasedSelector.ANTI_MONOTONE);
            }
            while (!this.defaultMailboxes.isEmpty()) {
                final Mailbox mailbox = this.defaultMailboxes.iterator().next();
                this.defaultMailboxes.remove(mailbox);
                mailbox.deliverAll(PhasedSelector.DEFAULT);
            }
        }

        // REDERIVE PHASE
        while (!this.rederivables.isEmpty()) {
            // re-derivable nodes take care of their unregistration!!
            final RederivableNode node = this.rederivables.iterator().next();
            node.rederiveOne();
        }

        // MONOTONE PHASE
        while (!this.monotoneMailboxes.isEmpty() || !this.defaultMailboxes.isEmpty()) {
            while (!this.monotoneMailboxes.isEmpty()) {
                final Mailbox mailbox = this.monotoneMailboxes.iterator().next();
                this.monotoneMailboxes.remove(mailbox);
                mailbox.deliverAll(PhasedSelector.MONOTONE);
            }
            while (!this.defaultMailboxes.isEmpty()) {
                final Mailbox mailbox = this.defaultMailboxes.iterator().next();
                this.defaultMailboxes.remove(mailbox);
                mailbox.deliverAll(PhasedSelector.DEFAULT);
            }
        }

        this.currentlyDelivering = false;
    }

    @Override
    public boolean isEmpty() {
        return this.rederivables.isEmpty() && this.antiMonotoneMailboxes.isEmpty()
                && this.monotoneMailboxes.isEmpty() && this.defaultMailboxes.isEmpty();
    }

    @Override
    public void notifyHasMessage(final Mailbox mailbox, final MessageSelector kind) {
        final Collection<Mailbox> mailboxes = getMailboxContainer(kind);
        mailboxes.add(mailbox);
        if (!this.isEnqueued && !this.currentlyDelivering) {
            this.tracker.activateUnenqueued(this);
        }
    }

    @Override
    public void notifyLostAllMessages(final Mailbox mailbox, final MessageSelector kind) {
        final Collection<Mailbox> mailboxes = getMailboxContainer(kind);
        mailboxes.remove(mailbox);
        if (isEmpty()) {
            this.tracker.deactivate(this);
        }
    }

    private Collection<Mailbox> getMailboxContainer(final MessageSelector kind) {
        if (kind == PhasedSelector.ANTI_MONOTONE) {
            return this.antiMonotoneMailboxes;
        } else if (kind == PhasedSelector.MONOTONE) {
            return this.monotoneMailboxes;
        } else if (kind == PhasedSelector.DEFAULT) {
            return this.defaultMailboxes;
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_MESSAGE_KIND + kind);
        }
    }

    public void addRederivable(final RederivableNode node) {
        this.rederivables.add(node);
        if (!this.isEnqueued) {
            this.tracker.activateUnenqueued(this);
        }
    }

    public void removeRederivable(final RederivableNode node) {
        this.rederivables.remove(node);
        if (isEmpty()) {
            this.tracker.deactivate(this);
        }
    }

    public Collection<RederivableNode> getRederivables() {
        return this.rederivables;
    }

    @Override
    public Map<MessageSelector, Collection<Mailbox>> getMailboxes() {
        Map<PhasedSelector, Collection<Mailbox>> map = new EnumMap<>(PhasedSelector.class);
        map.put(PhasedSelector.ANTI_MONOTONE, antiMonotoneMailboxes);
        map.put(PhasedSelector.MONOTONE, monotoneMailboxes);
        map.put(PhasedSelector.DEFAULT, defaultMailboxes);
        return Collections.unmodifiableMap(map);
    }

    @Override
    public boolean isRecursive() {
        return true;
    }

}
