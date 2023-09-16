/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.single;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

public abstract class TransformerNode extends SingleInputNode {

    public TransformerNode(final ReteContainer reteContainer) {
        super(reteContainer);
    }

    protected abstract Tuple transform(final Tuple input);

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
        for (Tuple ps : reteContainer.pullPropagatedContents(this, flush)) {
            collector.add(transform(ps));
        }
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        for (final Entry<Tuple, Timeline<Timestamp>> entry : reteContainer.pullPropagatedContentsWithTimestamp(this, flush).entrySet()) {
            collector.put(transform(entry.getKey()), entry.getValue());
        }
    }

    @Override
    public void update(final Direction direction, final Tuple updateElement, final Timestamp timestamp) {
        propagateUpdate(direction, transform(updateElement), timestamp);
    }

}
