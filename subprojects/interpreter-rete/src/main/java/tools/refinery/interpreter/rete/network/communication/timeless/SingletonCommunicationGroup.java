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
import java.util.Map;

import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.communication.PhasedSelector;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;

/**
 * A communication group containing only a single node with a single default
 * mailbox.
 *
 * @author Tamas Szabo
 * @since 1.6
 */
public class SingletonCommunicationGroup extends CommunicationGroup {

    private Mailbox mailbox;

    /**
     * @since 1.7
     */
    public SingletonCommunicationGroup(final CommunicationTracker tracker, final Node representative, final int identifier) {
        super(tracker, representative, identifier);
    }

    @Override
    public void deliverMessages() {
        this.mailbox.deliverAll(PhasedSelector.DEFAULT);
    }

    @Override
    public boolean isEmpty() {
        return this.mailbox == null;
    }

    @Override
    public void notifyHasMessage(final Mailbox mailbox, final MessageSelector kind) {
        if (kind == PhasedSelector.DEFAULT) {
            this.mailbox = mailbox;
            if (!this.isEnqueued) {
                this.tracker.activateUnenqueued(this);
            }
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_MESSAGE_KIND + kind);
        }
    }

    @Override
    public void notifyLostAllMessages(final Mailbox mailbox, final MessageSelector kind) {
        if (kind == PhasedSelector.DEFAULT) {
            this.mailbox = null;
            this.tracker.deactivate(this);
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_MESSAGE_KIND + kind);
        }
    }

    @Override
    public Map<MessageSelector, Collection<Mailbox>> getMailboxes() {
        if (mailbox != null) {
            return Collections.singletonMap(PhasedSelector.DEFAULT, Collections.singleton(mailbox));
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public boolean isRecursive() {
        return false;
    }

}
