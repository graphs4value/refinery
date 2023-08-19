/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.rete.single;

import java.util.Collection;
import java.util.Map;

import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContext;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.util.Direction;
import tools.refinery.viatra.runtime.matchers.util.timeline.Timeline;
import tools.refinery.viatra.runtime.rete.network.ReteContainer;
import tools.refinery.viatra.runtime.rete.network.Supplier;
import tools.refinery.viatra.runtime.rete.network.communication.Timestamp;

/**
 * A bucket holds a filtered set of tuples of its parent {@link DiscriminatorDispatcherNode}. 
 * Exactly those that have the given bucket key at their discrimination column.
 * 
 * <p> During operation, tuple contents and bucket keys have already been wrapped using {@link IQueryRuntimeContext#wrapElement(Object)}
 * 
 * @author Gabor Bergmann
 * @since 1.5
 */
public class DiscriminatorBucketNode extends SingleInputNode {

    private Object bucketKey;

    /**
     * @param bucketKey will be wrapped using {@link IQueryRuntimeContext#wrapElement(Object)}

     */
    public DiscriminatorBucketNode(ReteContainer reteContainer, Object bucketKey) {
        super(reteContainer);
        this.bucketKey = reteContainer.getNetwork().getEngine().getRuntimeContext().wrapElement(bucketKey);
    }

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
       if (parent != null) {
           getDispatcher().pullIntoFiltered(collector, bucketKey, flush);
       }
    }
    
    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        if (parent != null) {
            getDispatcher().pullIntoWithTimestampFiltered(collector, bucketKey, flush);
        }
    }

    @Override
    public void update(Direction direction, Tuple updateElement, Timestamp timestamp) {
        propagateUpdate(direction, updateElement, timestamp);
    }

    public Object getBucketKey() {
        return bucketKey;
    }
    
    @Override
    public void appendParent(Supplier supplier) {
        if (! (supplier instanceof DiscriminatorDispatcherNode))
            throw new IllegalArgumentException();
        super.appendParent(supplier);
    }
    
    public DiscriminatorDispatcherNode getDispatcher() {
        return (DiscriminatorDispatcherNode) parent;
    }
    
    @Override
    protected String toStringCore() {
        return String.format("%s<%s=='%s'>", 
                super.toStringCore(), 
                (getDispatcher() == null) ? "?" : getDispatcher().getDiscriminationColumnIndex(), 
                bucketKey);
    }
}
