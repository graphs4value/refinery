/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.delayed;

import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.Supplier;

public class DelayedConnectCommand extends DelayedCommand {

    public DelayedConnectCommand(final Supplier supplier, final Receiver receiver, final ReteContainer container) {
        super(supplier, receiver, Direction.INSERT, container);
    }

    @Override
    protected boolean isTimestampAware() {
        return this.container.isTimelyEvaluation() && this.container.getCommunicationTracker().areInSameGroup(this.supplier, this.receiver);
    }

}
