/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.index;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * A generic Indexer capable of indexing along any valid TupleMask. Does not keep track of parents, because will not
 * ever pull parents.
 *
 * @author Gabor Bergmann
 *
 */
public class GenericProjectionIndexer extends IndexerWithMemory implements ProjectionIndexer {

    public GenericProjectionIndexer(ReteContainer reteContainer, TupleMask mask) {
        super(reteContainer, mask);
    }

    @Override
    protected void update(Direction direction, Tuple updateElement, Tuple signature, boolean change,
            Timestamp timestamp) {
        propagate(direction, updateElement, signature, change, timestamp);
    }

    @Override
    public Collection<Tuple> get(Tuple signature) {
        return memory.get(signature);
    }

    @Override
    public Map<Tuple, Timeline<Timestamp>> getTimeline(Tuple signature) {
        return memory.getWithTimeline(signature);
    }

    @Override
    public Iterator<Tuple> iterator() {
        return memory.iterator();
    }

    @Override
    public Iterable<Tuple> getSignatures() {
        return memory.getSignatures();
    }

    /**
     * @since 2.0
     */
    @Override
    public int getBucketCount() {
        return memory.getKeysetSize();
    }

    @Override
    public Receiver getActiveNode() {
        return this;
    }

}
