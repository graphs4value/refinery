/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.rete.misc;

import java.util.Collection;
import java.util.Map;

import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContext;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.util.timeline.Timeline;
import tools.refinery.viatra.runtime.rete.network.ReteContainer;
import tools.refinery.viatra.runtime.rete.network.StandardNode;
import tools.refinery.viatra.runtime.rete.network.communication.Timestamp;

/**
 * Node that always contains a single constant Tuple
 * 
 * @author Gabor Bergmann
 */
public class ConstantNode extends StandardNode {

    protected Tuple constant;

    /**
     * @param constant
     *            will be wrapped using {@link IQueryRuntimeContext#wrapTuple(Tuple)}
     */
    public ConstantNode(ReteContainer reteContainer, Tuple constant) {
        super(reteContainer);
        this.constant = reteContainer.getNetwork().getEngine().getRuntimeContext().wrapTuple(constant);
    }

    @Override
    public void pullInto(Collection<Tuple> collector, boolean flush) {
        collector.add(constant);
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        collector.put(constant, Timestamp.INSERT_AT_ZERO_TIMELINE);
    }

}
