/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.mailbox.timeless;

import java.util.Map.Entry;

import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.communication.PhasedSelector;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.indexer.DefaultMessageIndexer;
import tools.refinery.interpreter.rete.network.mailbox.AdaptableMailbox;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;

/**
 * A mailbox implementation that splits updates messages according to the standard subset ordering into anti-monotonic
 * (deletions) and monotonic (insertions) updates.
 *
 * @author Tamas Szabo
 * @since 2.0
 */
public class UpdateSplittingMailbox extends AbstractUpdateSplittingMailbox<DefaultMessageIndexer, Receiver>
        implements AdaptableMailbox {

    protected Mailbox adapter;

    public UpdateSplittingMailbox(final Receiver receiver, final ReteContainer container) {
        super(receiver, container, DefaultMessageIndexer::new);
        this.adapter = this;
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
    public void postMessage(final Direction direction, final Tuple update, final Timestamp timestamp) {
        final DefaultMessageIndexer monotoneQueue = getActiveMonotoneQueue();
        final DefaultMessageIndexer antiMonotoneQueue = getActiveAntiMonotoneQueue();
        final boolean wasPresentAsMonotone = monotoneQueue.getCount(update) != 0;
        final boolean wasPresentAsAntiMonotone = antiMonotoneQueue.getCount(update) != 0;

        // it cannot happen that it was present in both
        assert !(wasPresentAsMonotone && wasPresentAsAntiMonotone);

        if (direction == Direction.INSERT) {
            if (wasPresentAsAntiMonotone) {
                // it was an anti-monotone one before
                antiMonotoneQueue.insert(update);
            } else {
                // it was a monotone one before or did not exist at all
                monotoneQueue.insert(update);
            }
        } else {
            if (wasPresentAsMonotone) {
                // it was a monotone one before
                monotoneQueue.delete(update);
            } else {
                // it was an anti-monotone one before or did not exist at all
                antiMonotoneQueue.delete(update);
            }
        }

        final Mailbox targetMailbox = this.adapter;
        final CommunicationGroup targetGroup = this.adapter.getCurrentGroup();

        if (antiMonotoneQueue.isEmpty()) {
            targetGroup.notifyLostAllMessages(targetMailbox, PhasedSelector.ANTI_MONOTONE);
        } else {
            targetGroup.notifyHasMessage(targetMailbox, PhasedSelector.ANTI_MONOTONE);
        }

        if (monotoneQueue.isEmpty()) {
            targetGroup.notifyLostAllMessages(targetMailbox, PhasedSelector.MONOTONE);
        } else {
            targetGroup.notifyHasMessage(targetMailbox, PhasedSelector.MONOTONE);
        }
    }

    @Override
    public void deliverAll(final MessageSelector kind) {
        if (kind == PhasedSelector.ANTI_MONOTONE) {
            // deliver anti-monotone
            this.deliveringAntiMonotone = true;
            for (final Entry<Tuple, Integer> entry : this.antiMonotoneQueue.getTuples().entrySet()) {
                final Tuple update = entry.getKey();
                final int count = entry.getValue();
                assert count < 0;
                for (int i = 0; i < Math.abs(count); i++) {
                    this.receiver.update(Direction.DELETE, update, Timestamp.ZERO);
                }
            }
            this.deliveringAntiMonotone = false;
            swapAndClearAntiMonotone();
        } else if (kind == PhasedSelector.MONOTONE) {
            // deliver monotone
            this.deliveringMonotone = true;
            for (final Entry<Tuple, Integer> entry : this.monotoneQueue.getTuples().entrySet()) {
                final Tuple update = entry.getKey();
                final int count = entry.getValue();
                assert count > 0;
                for (int i = 0; i < count; i++) {
                    this.receiver.update(Direction.INSERT, update, Timestamp.ZERO);
                }
            }
            this.deliveringMonotone = false;
            swapAndClearMonotone();
        } else {
            throw new IllegalArgumentException("Unsupported message kind " + kind);
        }
    }

    @Override
    public String toString() {
        return "US_MBOX (" + this.receiver + ") " + this.getActiveMonotoneQueue() + " "
                + this.getActiveAntiMonotoneQueue();
    }

}
