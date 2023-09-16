/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.network;

import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;

class UpdateMessage {
    public Receiver receiver;
    public Direction direction;
    public Tuple updateElement;

    public UpdateMessage(Receiver receiver, Direction direction, Tuple updateElement) {
        this.receiver = receiver;
        this.direction = direction;
        this.updateElement = updateElement;
    }

    @Override
    public String toString() {
        return "M." + direction + ": " + updateElement + " -> " + receiver;
    }

}
