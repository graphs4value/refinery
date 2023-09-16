/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.mailbox.timeless;

import java.util.Map;

import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.communication.PhasedSelector;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.mailbox.AdaptableMailbox;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;

/**
 * Default mailbox implementation.
 * <p>
 * Usually, the mailbox performs counting of messages so that they can cancel each other out. However, if marked as a
 * fall-through mailbox, than update messages are delivered directly to the receiver node to reduce overhead.
 *
 * @author Tamas Szabo
 * @since 2.0
 */
public class DefaultMailbox implements AdaptableMailbox {

    private static int SIZE_TRESHOLD = 127;

    protected Map<Tuple, Integer> queue;
    protected Map<Tuple, Integer> buffer;
    protected final Receiver receiver;
    protected final ReteContainer container;
    protected boolean delivering;
    protected Mailbox adapter;
    protected CommunicationGroup group;

    public DefaultMailbox(final Receiver receiver, final ReteContainer container) {
        this.receiver = receiver;
        this.container = container;
        this.queue = CollectionsFactory.createMap();
        this.buffer = CollectionsFactory.createMap();
        this.adapter = this;
    }

    protected Map<Tuple, Integer> getActiveQueue() {
        if (this.delivering) {
            return this.buffer;
        } else {
            return this.queue;
        }
    }

    @Override
    public Mailbox getAdapter() {
        return this.adapter;
    }

    @Override
    public void setAdapter(final Mailbox adapter) {
        this.adapter = adapter;
    }

    @Override
    public boolean isEmpty() {
        return getActiveQueue().isEmpty();
    }

    @Override
    public void postMessage(final Direction direction, final Tuple update, final Timestamp timestamp) {
        final Map<Tuple, Integer> activeQueue = getActiveQueue();
        final boolean wasEmpty = activeQueue.isEmpty();

        boolean significantChange = false;
        Integer count = activeQueue.get(update);
        if (count == null) {
            count = 0;
            significantChange = true;
        }

        if (direction == Direction.DELETE) {
            count--;
        } else {
            count++;
        }

        if (count == 0) {
            activeQueue.remove(update);
            significantChange = true;
        } else {
            activeQueue.put(update, count);
        }

        if (significantChange) {
            final Mailbox targetMailbox = this.adapter;
            final CommunicationGroup targetGroup = this.adapter.getCurrentGroup();

            if (wasEmpty) {
                targetGroup.notifyHasMessage(targetMailbox, PhasedSelector.DEFAULT);
            } else if (activeQueue.isEmpty()) {
                targetGroup.notifyLostAllMessages(targetMailbox, PhasedSelector.DEFAULT);
            }
        }
    }

    @Override
    public void deliverAll(final MessageSelector kind) {
        if (kind == PhasedSelector.DEFAULT) {
            // use the buffer during delivering so that there is a clear
            // separation between the stages
            this.delivering = true;
            this.receiver.batchUpdate(this.queue.entrySet(), Timestamp.ZERO);
            this.delivering = false;

            if (queue.size() > SIZE_TRESHOLD) {
                this.queue = this.buffer;
                this.buffer = CollectionsFactory.createMap();
            } else {
                this.queue.clear();
                final Map<Tuple, Integer> tmpQueue = this.queue;
                this.queue = this.buffer;
                this.buffer = tmpQueue;
            }
        } else {
            throw new IllegalArgumentException("Unsupported message kind " + kind);
        }
    }

    @Override
    public String toString() {
        return "D_MBOX (" + this.receiver + ") " + this.getActiveQueue();
    }

    @Override
    public Receiver getReceiver() {
        return this.receiver;
    }

    @Override
    public void clear() {
        this.queue.clear();
        this.buffer.clear();
    }

    @Override
    public CommunicationGroup getCurrentGroup() {
        return this.group;
    }

    @Override
    public void setCurrentGroup(final CommunicationGroup group) {
        this.group = group;
    }

}
