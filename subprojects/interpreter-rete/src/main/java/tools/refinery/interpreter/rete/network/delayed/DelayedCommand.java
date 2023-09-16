/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.delayed;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Signed;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.rete.network.Network;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.Supplier;

/**
 * Instances of this class are responsible for initializing a {@link Receiver} with the contents of a {@link Supplier}.
 * However, due to the dynamic nature of the Rete {@link Network} and to the fact that certain {@link Node}s in the
 * {@link Network} are sensitive to the shape of the {@link Network}, the commands must be delayed until the
 * construction of the {@link Network} has stabilized.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public abstract class DelayedCommand implements Runnable {

    protected final Supplier supplier;
    protected final Receiver receiver;
    protected final Direction direction;
    protected final ReteContainer container;

    public DelayedCommand(final Supplier supplier, final Receiver receiver, final Direction direction,
            final ReteContainer container) {
        this.supplier = supplier;
        this.receiver = receiver;
        this.direction = direction;
        this.container = container;
    }

    @Override
    public void run() {
        final CommunicationTracker tracker = this.container.getCommunicationTracker();
        final Mailbox mailbox = tracker.proxifyMailbox(this.supplier, this.receiver.getMailbox());

        if (this.isTimestampAware()) {
            final Map<Tuple, Timeline<Timestamp>> contents = this.container.pullContentsWithTimeline(this.supplier,
                    false);
            for (final Entry<Tuple, Timeline<Timestamp>> entry : contents.entrySet()) {
                for (final Signed<Timestamp> change : entry.getValue().asChangeSequence()) {
                    mailbox.postMessage(change.getDirection().multiply(this.direction), entry.getKey(),
                            change.getPayload());
                }
            }
        } else {
            final Collection<Tuple> contents = this.container.pullContents(this.supplier, false);
            for (final Tuple tuple : contents) {
                mailbox.postMessage(this.direction, tuple, Timestamp.ZERO);
            }
        }
    }

    @Override
    public String toString() {
        return this.supplier + " -> " + this.receiver.toString();
    }

    protected abstract boolean isTimestampAware();

}
