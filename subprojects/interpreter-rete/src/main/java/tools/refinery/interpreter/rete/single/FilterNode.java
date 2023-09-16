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

/**
 * This node implements a simple filter. A stateless abstract check() predicate determines whether a matching is allowed
 * to pass.
 *
 * @author Gabor Bergmann
 *
 */
public abstract class FilterNode extends SingleInputNode {

    public FilterNode(final ReteContainer reteContainer) {
        super(reteContainer);
    }

    /**
     * Abstract filtering predicate. Expected to be stateless.
     *
     * @param ps
     *            the matching to be checked.
     * @return true if and only if the parameter matching is allowed to pass through this node.
     */
    public abstract boolean check(final Tuple ps);

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
        for (final Tuple ps : this.reteContainer.pullPropagatedContents(this, flush)) {
            if (check(ps)) {
                collector.add(ps);
            }
        }
    }

    @Override
    public void pullIntoWithTimeline(Map<Tuple, Timeline<Timestamp>> collector, boolean flush) {
        for (final Entry<Tuple, Timeline<Timestamp>> entry : this.reteContainer.pullPropagatedContentsWithTimestamp(this, flush).entrySet()) {
            if (check(entry.getKey())) {
                collector.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void update(final Direction direction, final Tuple updateElement, final Timestamp timestamp) {
        if (check(updateElement)) {
            propagateUpdate(direction, updateElement, timestamp);
        }
    }

}
