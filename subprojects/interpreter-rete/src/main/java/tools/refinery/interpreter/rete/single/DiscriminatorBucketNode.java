/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.single;

import java.util.Collection;
import java.util.Map;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.Supplier;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

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
