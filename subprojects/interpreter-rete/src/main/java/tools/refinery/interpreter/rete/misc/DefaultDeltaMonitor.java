/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.misc;

import tools.refinery.interpreter.rete.network.Network;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * Default configuration for DeltaMonitor.
 *
 * @author Gabor Bergmann
 *
 */
public class DefaultDeltaMonitor extends DeltaMonitor<Tuple> {

    /**
     * @param reteContainer
     */
    public DefaultDeltaMonitor(ReteContainer reteContainer) {
        super(reteContainer);
    }

    /**
     * @param network
     */
    public DefaultDeltaMonitor(Network network) {
        super(network.getHeadContainer());
    }

    @Override
    public Tuple statelessConvert(Tuple tuple) {
        return tuple;
    }

}
