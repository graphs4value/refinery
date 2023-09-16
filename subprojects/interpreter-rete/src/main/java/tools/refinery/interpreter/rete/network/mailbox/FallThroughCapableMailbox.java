/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.mailbox;

import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.network.Receiver;

/**
 * A fall through capable mailbox can directly call the update method of its {@link Receiver} instead of using the
 * standard post-deliver mailbox semantics. If the fall through flag is set to true, the mailbox uses direct delivery,
 * otherwise it operates in the original behavior. The fall through operation is preferable whenever applicable because
 * it improves performance. The fall through flag is controlled by the {@link CommunicationTracker} based on the
 * receiver node type and network topology.
 *
 * @author Tamas Szabo
 * @since 2.2
 */
public interface FallThroughCapableMailbox extends Mailbox {

    public boolean isFallThrough();

    public void setFallThrough(final boolean fallThrough);

}
