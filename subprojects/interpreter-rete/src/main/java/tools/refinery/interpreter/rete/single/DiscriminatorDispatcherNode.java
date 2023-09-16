/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.single;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.interpreter.rete.network.NetworkStructureChangeSensitiveNode;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * Node that sends tuples off to different buckets (attached as children of type {@link DiscriminatorBucketNode}), based
 * on the value of a given column.
 *
 * <p>
 * Tuple contents and bucket keys have already been wrapped using {@link IQueryRuntimeContext#wrapElement(Object)}
 *
 * @author Gabor Bergmann
 * @since 1.5
 */
public class DiscriminatorDispatcherNode extends SingleInputNode implements NetworkStructureChangeSensitiveNode {

    private int discriminationColumnIndex;
    private Map<Object, DiscriminatorBucketNode> buckets = new HashMap<>();
    private Map<Object, Mailbox> bucketMailboxes = new HashMap<>();

    /**
     * @param reteContainer
     */
    public DiscriminatorDispatcherNode(ReteContainer reteContainer, int discriminationColumnIndex) {
        super(reteContainer);
        this.discriminationColumnIndex = discriminationColumnIndex;
    }

    @Override
    public void update(Direction direction, Tuple updateElement, Timestamp timestamp) {
        Object dispatchKey = updateElement.get(discriminationColumnIndex);
        Mailbox bucketMailBox = bucketMailboxes.get(dispatchKey);
        if (bucketMailBox != null) {
            bucketMailBox.postMessage(direction, updateElement, timestamp);
        }
    }

    public int getDiscriminationColumnIndex() {
        return discriminationColumnIndex;
    }

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
        propagatePullInto(collector, flush);
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        propagatePullIntoWithTimestamp(collector, flush);
    }

    /**
     * @since 2.3
     */
    public void pullIntoFiltered(final Collection<Tuple> collector, final Object bucketKey, final boolean flush) {
        final ArrayList<Tuple> unfiltered = new ArrayList<Tuple>();
        propagatePullInto(unfiltered, flush);
        for (Tuple tuple : unfiltered) {
            if (bucketKey.equals(tuple.get(discriminationColumnIndex))) {
                collector.add(tuple);
            }
        }
    }

    /**
     * @since 2.3
     */
    public void pullIntoWithTimestampFiltered(final Map<Tuple, Timeline<Timestamp>> collector, final Object bucketKey,
            final boolean flush) {
        final Map<Tuple, Timeline<Timestamp>> unfiltered = CollectionsFactory.createMap();
        propagatePullIntoWithTimestamp(unfiltered, flush);
        for (final Entry<Tuple, Timeline<Timestamp>> entry : unfiltered.entrySet()) {
            if (bucketKey.equals(entry.getKey().get(discriminationColumnIndex))) {
                collector.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void appendChild(Receiver receiver) {
        super.appendChild(receiver);
        if (receiver instanceof DiscriminatorBucketNode) {
            DiscriminatorBucketNode bucket = (DiscriminatorBucketNode) receiver;
            Object bucketKey = bucket.getBucketKey();
            DiscriminatorBucketNode old = buckets.put(bucketKey, bucket);
            if (old != null) {
                throw new IllegalStateException();
            }
            bucketMailboxes.put(bucketKey, this.getCommunicationTracker().proxifyMailbox(this, bucket.getMailbox()));
        }
    }

    /**
     * @since 2.2
     */
    public Map<Object, Mailbox> getBucketMailboxes() {
        return this.bucketMailboxes;
    }

    @Override
    public void networkStructureChanged() {
        bucketMailboxes.clear();
        for (Receiver receiver : children) {
            if (receiver instanceof DiscriminatorBucketNode) {
                DiscriminatorBucketNode bucket = (DiscriminatorBucketNode) receiver;
                Object bucketKey = bucket.getBucketKey();
                bucketMailboxes.put(bucketKey,
                        this.getCommunicationTracker().proxifyMailbox(this, bucket.getMailbox()));
            }
        }
    }

    @Override
    public void removeChild(Receiver receiver) {
        super.removeChild(receiver);
        if (receiver instanceof DiscriminatorBucketNode) {
            DiscriminatorBucketNode bucket = (DiscriminatorBucketNode) receiver;
            Object bucketKey = bucket.getBucketKey();
            DiscriminatorBucketNode old = buckets.remove(bucketKey);
            if (old != bucket)
                throw new IllegalStateException();
            bucketMailboxes.remove(bucketKey);
        }
    }

    @Override
    protected String toStringCore() {
        return super.toStringCore() + '<' + discriminationColumnIndex + '>';
    }

}
