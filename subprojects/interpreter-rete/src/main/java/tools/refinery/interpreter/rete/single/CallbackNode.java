/*******************************************************************************
 * Copyright (c) 2010-2012, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.single;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.backend.IUpdateable;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.misc.SimpleReceiver;

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
