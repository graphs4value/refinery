/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.mailbox.timeless;

import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.communication.timeless.TimelessCommunicationTracker;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.mailbox.AdaptableMailbox;
import tools.refinery.interpreter.rete.network.mailbox.FallThroughCapableMailbox;

/**
 * This mailbox changes its behavior based on the position of its {@link Receiver} in the network topology.
 * It either behaves as a {@link DefaultMailbox} or as an {@link UpdateSplittingMailbox}. The decision is made by the
 * {@link CommunicationTracker}, see {@link TimelessCommunicationTracker#postProcessNode(Node)} for more details.
 *
 * @author Tamas Szabo
 */
public class BehaviorChangingMailbox implements FallThroughCapableMailbox {

    protected boolean fallThrough;
    protected boolean split;
    protected AdaptableMailbox wrapped;
    protected final Receiver receiver;
    protected final ReteContainer container;
    protected CommunicationGroup group;

    public BehaviorChangingMailbox(final Receiver receiver, final ReteContainer container) {
        this.fallThrough = false;
        this.split = false;
        this.receiver = receiver;
        this.container = container;
        this.wrapped = new DefaultMailbox(receiver, container);
        this.wrapped.setAdapter(this);
    }

    @Override
    public void postMessage(final Direction direction, final Tuple update, final Timestamp timestamp) {
        if (this.fallThrough && !this.container.isExecutingDelayedCommands()) {
            // disable fall through while we are in the middle of executing delayed construction commands
            this.receiver.update(direction, update, timestamp);
        } else {
            this.wrapped.postMessage(direction, update, timestamp);
        }
    }

    @Override
    public void deliverAll(final MessageSelector kind) {
        this.wrapped.deliverAll(kind);
    }

    @Override
    public String toString() {
        return "A_MBOX -> " + this.wrapped;
    }

    public void setSplitFlag(final boolean splitValue) {
        if (this.split != splitValue) {
            assert isEmpty();
            if (splitValue) {
                this.wrapped = new UpdateSplittingMailbox(this.receiver, this.container);
            } else {
                this.wrapped = new DefaultMailbox(this.receiver, this.container);
            }
            this.wrapped.setAdapter(this);
            this.split = splitValue;
        }
    }

    @Override
    public boolean isEmpty() {
        return this.wrapped.isEmpty();
    }

    @Override
    public void clear() {
        this.wrapped.clear();
    }

    @Override
    public Receiver getReceiver() {
        return this.receiver;
    }

    @Override
    public CommunicationGroup getCurrentGroup() {
        return this.group;
    }

    @Override
    public void setCurrentGroup(final CommunicationGroup group) {
        this.group = group;
    }

    @Override
    public boolean isFallThrough() {
        return this.fallThrough;
    }

    @Override
    public void setFallThrough(final boolean fallThrough) {
        this.fallThrough = fallThrough;
    }

}
