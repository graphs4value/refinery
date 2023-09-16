/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.remote;

import java.util.Collection;
import java.util.Map;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.single.SingleInputNode;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * This node receives updates from a remote supplier; no local updates are expected.
 *
 * @author Gabor Bergmann
 *
 */
public class RemoteSupplier extends SingleInputNode {

    RemoteReceiver counterpart;

    public RemoteSupplier(ReteContainer reteContainer, RemoteReceiver counterpart) {
        super(reteContainer);
        this.counterpart = counterpart;
        counterpart.addTarget(reteContainer.makeAddress(this));
    }

    @Override
    public void pullInto(Collection<Tuple> collector, boolean flush) {
        Collection<Tuple> pulled = counterpart.remotePull(flush);
        collector.addAll(pulled);
    }

    @Override
    public void pullIntoWithTimeline(Map<Tuple, Timeline<Timestamp>> collector, boolean flush) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(Direction direction, Tuple updateElement, Timestamp timestamp) {
        propagateUpdate(direction, updateElement, timestamp);
    }

}
