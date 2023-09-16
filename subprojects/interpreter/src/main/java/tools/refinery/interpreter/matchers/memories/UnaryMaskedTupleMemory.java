/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.memories;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.interpreter.matchers.util.IMemoryView;
import tools.refinery.interpreter.matchers.util.IMultiLookup;
import tools.refinery.interpreter.matchers.util.IMultiLookup.ChangeGranularity;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * Specialized for unary mask; tuples are indexed by a single column as opposed to a projection (signature) tuple.
 *
 * @author Gabor Bergmann
 * @since 2.0
 */
public final class UnaryMaskedTupleMemory<Timestamp extends Comparable<Timestamp>> extends MaskedTupleMemory<Timestamp> {

    protected IMultiLookup<Object, Tuple> columnToTuples;
    protected final int keyPosition;

    /**
     * @param mask
     *            The mask used to index the matchings
     * @param owner the object "owning" this memory
     * @param bucketType the kind of tuple collection maintained for each indexer bucket
     * @since 2.0
     */
    public UnaryMaskedTupleMemory(TupleMask mask, MemoryType bucketType, Object owner) {
        super(mask, owner);
        if (1 != mask.getSize()) throw new IllegalArgumentException(mask.toString());

        columnToTuples = CollectionsFactory.<Object, Tuple>createMultiLookup(
                Object.class, bucketType, Object.class);
        keyPosition = mask.indices[0];
    }

    @Override
    public void clear() {
        columnToTuples.clear();
    }

    @Override
    public int getKeysetSize() {
        return columnToTuples.countKeys();
    }

    @Override
    public int getTotalSize() {
        int i = 0;
        for (Object key : columnToTuples.distinctKeys()) {
            i += columnToTuples.lookup(key).size();
        }
        return i;
    }

    @Override
    public Iterator<Tuple> iterator() {
        return columnToTuples.distinctValues().iterator();
    }

    @Override
    public Iterable<Tuple> getSignatures() {
        return () -> {
            Iterator<Object> wrapped = columnToTuples.distinctKeys().iterator();
            return new Iterator<Tuple>() {
                @Override
                public boolean hasNext() {
                    return wrapped.hasNext();
                }
                @Override
                public Tuple next() {
                    Object key = wrapped.next();
                    return Tuples.staticArityFlatTupleOf(key);
                }
            };
        };
    }

    @Override
    public Collection<Tuple> get(ITuple signature) {
        Object key = signature.get(0);
        IMemoryView<Tuple> bucket = columnToTuples.lookup(key);
        return bucket == null ? null : bucket.distinctValues();
    }

    @Override
    public Map<Tuple, Timeline<Timestamp>> getWithTimeline(ITuple signature) {
        throw new UnsupportedOperationException("Timeless memories do not support timestamp-based lookup!");
    }

    @Override
    public boolean remove(Tuple tuple, Tuple signature) {
        return removeInternal(tuple, tuple.get(keyPosition));
    }

    @Override
    public boolean remove(Tuple tuple) {
        return removeInternal(tuple, tuple.get(keyPosition));
    }

    @Override
    public boolean add(Tuple tuple, Tuple signature) {
        return addInternal(tuple, tuple.get(keyPosition));
    }

    @Override
    public boolean add(Tuple tuple) {
        return addInternal(tuple, tuple.get(keyPosition));
    }

    protected boolean addInternal(Tuple tuple, Object key) {
        try {
            return columnToTuples.addPair(key, tuple) == ChangeGranularity.KEY;
        } catch (IllegalStateException ex) { // ignore worthless internal exception details
            throw raiseDuplicateInsertion(tuple);
        }
    }

    protected boolean removeInternal(Tuple tuple, Object key) {
        try {
            return columnToTuples.removePair(key, tuple) == ChangeGranularity.KEY;
        } catch (IllegalStateException ex) { // ignore worthless internal exception details
            throw raiseDuplicateDeletion(tuple);
        }
    }

}
