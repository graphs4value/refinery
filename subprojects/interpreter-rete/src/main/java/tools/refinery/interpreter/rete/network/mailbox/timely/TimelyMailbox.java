/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.mailbox.timely;

import java.util.Map;
import java.util.TreeMap;

import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.communication.timely.ResumableNode;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.matcher.TimelyConfiguration.TimelineRepresentation;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;

public class TimelyMailbox implements Mailbox {

    protected TreeMap<Timestamp, Map<Tuple, Integer>> queue;
    protected final Receiver receiver;
    protected final ReteContainer container;
    protected CommunicationGroup group;
    protected boolean fallThrough;

    public TimelyMailbox(final Receiver receiver, final ReteContainer container) {
        this.receiver = receiver;
        this.container = container;
        this.queue = CollectionsFactory.createTreeMap();
    }

    protected TreeMap<Timestamp, Map<Tuple, Integer>> getActiveQueue() {
        return this.queue;
    }

    @Override
    public boolean isEmpty() {
        return getActiveQueue().isEmpty();
    }

    @Override
    public void postMessage(final Direction direction, final Tuple update, final Timestamp timestamp) {
        final TreeMap<Timestamp, Map<Tuple, Integer>> activeQueue = getActiveQueue();

        Map<Tuple, Integer> tupleMap = activeQueue.get(timestamp);
        final boolean wasEmpty = tupleMap == null;
        boolean significantChange = false;

        if (tupleMap == null) {
            tupleMap = CollectionsFactory.createMap();
            activeQueue.put(timestamp, tupleMap);
            significantChange = true;
        }

        Integer count = tupleMap.get(update);
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
            tupleMap.remove(update);
            if (tupleMap.isEmpty()) {
                activeQueue.remove(timestamp);
            }
            significantChange = true;
        } else {
            tupleMap.put(update, count);
        }

        if (significantChange) {
            if (wasEmpty) {
                this.group.notifyHasMessage(this, timestamp);
            } else if (tupleMap.isEmpty()) {
                final Timestamp resumableTimestamp = (this.receiver instanceof ResumableNode)
                        ? ((ResumableNode) this.receiver).getResumableTimestamp()
                        : null;
                // check if there is folding left to do before unsubscribing just based on the message queue being empty
                if (resumableTimestamp == null || resumableTimestamp.compareTo(timestamp) != 0) {
                    this.group.notifyLostAllMessages(this, timestamp);
                }
            }
        }
    }

    @Override
    public void deliverAll(final MessageSelector selector) {
        if (selector instanceof Timestamp) {
            final Timestamp timestamp = (Timestamp) selector;
            // REMOVE the tuples associated with the selector, dont just query them
            final Map<Tuple, Integer> tupleMap = this.queue.remove(timestamp);

            // tupleMap may be empty if we only have lazy folding to do
            if (tupleMap != null) {
                this.receiver.batchUpdate(tupleMap.entrySet(), timestamp);
            }

            if (this.container.getTimelyConfiguration()
                    .getTimelineRepresentation() == TimelineRepresentation.FAITHFUL) {
                // (1) either normal delivery, which ended up being a lazy folding state
                // (2) and/or lazy folding needs to be resumed
                if (this.receiver instanceof ResumableNode) {
                    ((ResumableNode) this.receiver).resumeAt(timestamp);
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported message selector " + selector);
        }
    }

    @Override
    public String toString() {
        return "DDF_MBOX (" + this.receiver + ") " + this.getActiveQueue();
    }

    @Override
    public Receiver getReceiver() {
        return this.receiver;
    }

    @Override
    public void clear() {
        this.queue.clear();
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
