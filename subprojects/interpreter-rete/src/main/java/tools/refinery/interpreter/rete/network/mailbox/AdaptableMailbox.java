/*******************************************************************************
 * Copyright (c) 2010-2018, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.mailbox;

import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.network.communication.timely.TimelyMailboxProxy;
import tools.refinery.interpreter.rete.network.mailbox.timeless.BehaviorChangingMailbox;

/**
 * An adaptable mailbox can be wrapped by another mailbox to act in behalf of that. The significance of the adaptation
 * is that the adaptee will notify the {@link CommunicationTracker} about updates by promoting the adapter itself.
 * Adaptable mailboxes are used by the {@link BehaviorChangingMailbox}.
 *
 * Compare this with {@link TimelyMailboxProxy}. That one also wraps another mailbox in order to
 * perform preprocessing on the messages sent to the original recipient.
 *
 * @author Tamas Szabo
 * @since 2.0
 */
public interface AdaptableMailbox extends Mailbox {

    public Mailbox getAdapter();

    public void setAdapter(final Mailbox adapter);

}
