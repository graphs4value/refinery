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

public class DelayedDisconnectCommand extends DelayedCommand {

    protected final boolean wasInSameSCC;

    public DelayedDisconnectCommand(final Supplier supplier, final Receiver receiver, final ReteContainer container, final boolean wasInSameSCC) {
        super(supplier, receiver, Direction.DELETE, container);
        this.wasInSameSCC = wasInSameSCC;
    }

    @Override
    protected boolean isTimestampAware() {
        return this.wasInSameSCC;
    }

}
