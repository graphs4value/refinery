/*******************************************************************************
 * Copyright (c) 2010-2018, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.mailbox.timeless;

import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.indexer.MessageIndexer;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.rete.network.mailbox.MessageIndexerFactory;

/**
 * An abstract mailbox implementation that is capable of splitting update messages based on some form of monotonicity
 * (anti-monotone and monotone). The monotonicity is either defined by the less or equal operator of a poset or, it can
 * be the standard subset ordering among sets of tuples.
 *
 * @author Tamas Szabo
 * @since 2.0
 *
 */
public abstract class AbstractUpdateSplittingMailbox<IndexerType extends MessageIndexer, ReceiverType extends Receiver> implements Mailbox {

    protected IndexerType monotoneQueue;
    protected IndexerType antiMonotoneQueue;
    protected IndexerType monotoneBuffer;
    protected IndexerType antiMonotoneBuffer;
    protected boolean deliveringMonotone;
    protected boolean deliveringAntiMonotone;
    protected final ReceiverType receiver;
    protected final ReteContainer container;
    protected CommunicationGroup group;

    public AbstractUpdateSplittingMailbox(final ReceiverType receiver, final ReteContainer container,
            final MessageIndexerFactory<IndexerType> factory) {
        this.receiver = receiver;
        this.container = container;
        this.monotoneQueue = factory.create();
        this.antiMonotoneQueue = factory.create();
        this.monotoneBuffer = factory.create();
        this.antiMonotoneBuffer = factory.create();
        this.deliveringMonotone = false;
        this.deliveringAntiMonotone = false;
    }

    protected void swapAndClearMonotone() {
        final IndexerType tmp = this.monotoneQueue;
        this.monotoneQueue = this.monotoneBuffer;
        this.monotoneBuffer = tmp;
        this.monotoneBuffer.clear();
    }

    protected void swapAndClearAntiMonotone() {
        final IndexerType tmp = this.antiMonotoneQueue;
        this.antiMonotoneQueue = this.antiMonotoneBuffer;
        this.antiMonotoneBuffer = tmp;
        this.antiMonotoneBuffer.clear();
    }

    protected IndexerType getActiveMonotoneQueue() {
        if (this.deliveringMonotone) {
            return this.monotoneBuffer;
        } else {
            return this.monotoneQueue;
        }
    }

    protected IndexerType getActiveAntiMonotoneQueue() {
        if (this.deliveringAntiMonotone) {
            return this.antiMonotoneBuffer;
        } else {
            return this.antiMonotoneQueue;
        }
    }

    @Override
    public ReceiverType getReceiver() {
        return this.receiver;
    }

    @Override
    public void clear() {
        this.monotoneQueue.clear();
        this.antiMonotoneQueue.clear();
        this.monotoneBuffer.clear();
        this.antiMonotoneBuffer.clear();
    }

    @Override
    public boolean isEmpty() {
        return this.getActiveMonotoneQueue().isEmpty() && this.getActiveAntiMonotoneQueue().isEmpty();
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
