/*******************************************************************************
 * Copyright (c) 2010-2012, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.rete.single;

import tools.refinery.viatra.runtime.matchers.backend.IUpdateable;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.util.Direction;
import tools.refinery.viatra.runtime.rete.misc.SimpleReceiver;
import tools.refinery.viatra.runtime.rete.network.ReteContainer;
import tools.refinery.viatra.runtime.rete.network.communication.Timestamp;

/**
 * @author Bergmann Gabor
 * 
 */
public class CallbackNode extends SimpleReceiver {

    IUpdateable updateable;

    public CallbackNode(ReteContainer reteContainer, IUpdateable updateable) 
    {
        super(reteContainer);
        this.updateable = updateable;
    }

    @Override
    public void update(Direction direction, Tuple updateElement, Timestamp timestamp) {
        updateable.update(updateElement, direction == Direction.INSERT);
    }

}
