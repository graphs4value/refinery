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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import tools.refinery.interpreter.matchers.memories.timely.TimelyDefaultMaskedTupleMemory;
import tools.refinery.interpreter.matchers.memories.timely.TimelyIdentityMaskedTupleMemory;
import tools.refinery.interpreter.matchers.memories.timely.TimelyNullaryMaskedTupleMemory;
import tools.refinery.interpreter.matchers.memories.timely.TimelyUnaryMaskedTupleMemory;
import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Clearable;
import tools.refinery.interpreter.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.interpreter.matchers.util.resumable.MaskedResumable;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * Indexes a collection of tuples by their signature (i.e. footprint, projection) obtained according to a mask. May
 * belong to an "owner" (for documentation / traceability purposes).
 * <p>
 * There are timeless and timely versions of the different memories. Timely versions associate {@link Timeline}s with
 * the stored tuples.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @author Gabor Bergmann
 * @author Tamas Szabo
 * @since 2.0
 */
public abstract class MaskedTupleMemory<Timestamp extends Comparable<Timestamp>>
        implements Clearable, MaskedResumable<Timestamp> {

    /**
     * Creates a new memory for the given owner that indexes tuples according to the given mask.
     */
    public static <T extends Comparable<T>> MaskedTupleMemory<T> create(final TupleMask mask,
            final MemoryType bucketType, final Object owner) {
        return create(mask, bucketType, owner, false);
    }

    /**
     * Creates a new memory for the given owner that indexes tuples according to the given mask. Clients can specify if
     * the created memory should be timely or not. <br>
     * <br>
     * Timely means that tuples are associated with a timeline.
     *
     * @since 2.3
     */
    public static <T extends Comparable<T>> MaskedTupleMemory<T> create(final TupleMask mask,
            final MemoryType bucketType, final Object owner, final boolean isTimely) {
        return create(mask, bucketType, owner, isTimely, false);
    }

    /**
     * Creates a new memory for the given owner that indexes tuples according to the given mask. Clients can specify if
     * the created memory should be timely or not. In case of timely memory, clients can also specify if the memory is
     * lazy or not. <br>
     * <br>
     * Timely means that tuples are associated with a timeline. <br>
     * <br>
     * Lazyness can only be used together with timely memories. It means that the maintenance of the timelines is lazy,
     * that is, the memory only updates its internal data structures at the timestamp affected by an update, and can be
     * instructed later to resume the maintenance at higher timestamps, as well.
     *
     * @since 2.4
     */
    public static <T extends Comparable<T>> MaskedTupleMemory<T> create(final TupleMask mask,
            final MemoryType bucketType, final Object owner, final boolean isTimely, final boolean isLazy) {
        if (isTimely) {
            if (bucketType != MemoryType.SETS) {
                throw new IllegalArgumentException("Timely memories only support SETS as the bucket type!");
            }
            if (mask.isIdentity()) {
                return new TimelyIdentityMaskedTupleMemory<T>(mask, owner, isLazy);
            } else if (0 == mask.getSize()) {
                return new TimelyNullaryMaskedTupleMemory<T>(mask, owner, isLazy);
            } else if (1 == mask.getSize()) {
                return new TimelyUnaryMaskedTupleMemory<T>(mask, owner, isLazy);
            } else {
                return new TimelyDefaultMaskedTupleMemory<T>(mask, owner, isLazy);
            }
        } else {
            if (isLazy) {
                throw new IllegalArgumentException("Lazy maintenance is only supported by timely memories!");
            }
            if (mask.isIdentity()) {
                return new IdentityMaskedTupleMemory<T>(mask, bucketType, owner);
            } else if (0 == mask.getSize()) {
                return new NullaryMaskedTupleMemory<T>(mask, bucketType, owner);
            } else if (1 == mask.getSize()) {
                return new UnaryMaskedTupleMemory<T>(mask, bucketType, owner);
            } else {
                return new DefaultMaskedTupleMemory<T>(mask, bucketType, owner);
            }
        }
    }

    @Override
    public Map<Tuple, Map<Tuple, Diff<Timestamp>>> resumeAt(final Timestamp timestamp) {
        throw new UnsupportedOperationException("This is only supported by lazy timely memory implementations!");
    }

    @Override
    public Iterable<Tuple> getResumableSignatures() {
        throw new UnsupportedOperationException("This is only supported by lazy timely memory implementations!");
    }

    @Override
    public Timestamp getResumableTimestamp() {
        return null;
    }

    /**
     * Initializes the contents of this memory based on the contents of another memory. The default value is associated
     * with each tuple in the timely memories.
     *
     * @since 2.3
     */
    public void initializeWith(final MaskedTupleMemory<Timestamp> other, final Timestamp defaultValue) {
        throw new UnsupportedOperationException("This is only supported by timely memory implementations!");
    }

    /**
     * Returns true if there is any tuple with the given signature that is present at the timestamp +inf, false
     * otherwise.
     * @since 2.4
     */
    public boolean isPresentAtInfinity(final ITuple signature) {
        return get(signature) != null;
    }

    /**
     * Returns true of this memory is timely, false otherwise.
     *
     * @since 2.3
     */
    public boolean isTimely() {
        return false;
    }

    /**
     * The mask by which the tuples are indexed.
     */
    protected final TupleMask mask;

    /**
     * The object "owning" this memory. May be null.
     *
     * @since 1.7
     */
    protected final Object owner;

    /**
     * The node owning this memory. May be null.
     *
     * @since 2.0
     */
    public Object getOwner() {
        return owner;
    }

    /**
     * The mask according to which tuples are projected and indexed.
     *
     * @since 2.0
     */
    public TupleMask getMask() {
        return mask;
    }

    /**
     * @return the number of distinct signatures of all stored tuples.
     */
    public abstract int getKeysetSize();

    /**
     * @return the total number of distinct tuples stored. Multiple copies of the same tuple, if allowed, are counted as
     *         one.
     *
     *         <p>
     *         This is currently not cached but computed on demand. It is therefore not efficient, and shall only be
     *         used for debug / profiling purposes.
     */
    public abstract int getTotalSize();

    /**
     * Iterates over distinct tuples stored in the memory, regardless of their signatures.
     */
    public abstract Iterator<Tuple> iterator();

    /**
     * Retrieves a read-only view of exactly those signatures for which at least one tuple is stored
     *
     * @since 2.0
     */
    public abstract Iterable<Tuple> getSignatures();

    /**
     * Retrieves tuples that have the specified signature
     *
     * @return collection of tuples found, null if none
     */
    public abstract Collection<Tuple> get(final ITuple signature);

    /**
     * Retrieves the tuples and their associated timelines that have the specified signature.
     *
     * @return the mappings from tuples to timelines, null if there is no mapping for the signature
     * @since 2.4
     */
    public abstract Map<Tuple, Timeline<Timestamp>> getWithTimeline(final ITuple signature);

    /**
     * Retrieves tuples that have the specified signature.
     *
     * @return collection of tuples found, never null
     * @since 2.1
     */
    public Collection<Tuple> getOrEmpty(final ITuple signature) {
        final Collection<Tuple> result = get(signature);
        return result == null ? Collections.emptySet() : result;
    }

    /**
     * Retrieves tuples with their associated timelines that have the specified signature.
     *
     * @return map of tuples and timelines found, never null
     * @since 2.4
     */
    public Map<Tuple, Timeline<Timestamp>> getOrEmptyWithTimeline(final ITuple signature) {
        final Map<Tuple, Timeline<Timestamp>> result = getWithTimeline(signature);
        return result == null ? Collections.emptyMap() : result;
    }

    /**
     * Removes a tuple occurrence from the memory with the given signature.
     *
     * @param tuple
     *            the tuple to be removed from the memory
     * @param signature
     *            precomputed footprint of the tuple according to the mask
     *
     * @return true if this was the the last occurrence of the signature (according to the mask)
     */
    public boolean remove(final Tuple tuple, final Tuple signature) {
        throw new UnsupportedOperationException("This is only supported by timeless memory implementations!");
    }

    /**
     * Removes a tuple occurrence from the memory with the given signature and timestamp.
     *
     * @param tuple
     *            the tuple to be removed from the memory
     * @param signature
     *            precomputed footprint of the tuple according to the mask
     * @param timestamp
     *            the timestamp associated with the tuple
     *
     * @return A {@link Diff} describing how the timeline of the given tuple changed.
     *
     * @since 2.4
     */
    public Diff<Timestamp> removeWithTimestamp(final Tuple tuple, final Tuple signature, final Timestamp timestamp) {
        throw new UnsupportedOperationException("This is only supported by timely memory implementations!");
    }

    /**
     * Removes a tuple occurrence from the memory.
     *
     * @param tuple
     *            the tuple to be removed from the memory
     *
     * @return true if this was the the last occurrence of the signature (according to the mask)
     */
    public boolean remove(final Tuple tuple) {
        throw new UnsupportedOperationException("This is only supported by timeless memory implementations!");
    }

    /**
     * Removes a tuple occurrence from the memory with the given timestamp.
     *
     * @param tuple
     *            the tuple to be removed from the memory
     * @param timestamp
     *            the timestamp associated with the tuple
     *
     * @return A {@link Diff} describing how the timeline of the given tuple changed.
     *
     * @since 2.4
     */
    public Diff<Timestamp> removeWithTimestamp(final Tuple tuple, final Timestamp timestamp) {
        throw new UnsupportedOperationException("This is only supported by timely memory implementations!");
    }

    /**
     * Adds a tuple occurrence to the memory with the given signature.
     *
     * @param tuple
     *            the tuple to be added to the memory
     * @param signature
     *            precomputed footprint of the tuple according to the mask
     *
     * @return true if new signature encountered (according to the mask)
     */
    public boolean add(final Tuple tuple, final Tuple signature) {
        throw new UnsupportedOperationException("This is only supported by timeless memory implementations!");
    }

    /**
     * Adds a tuple occurrence to the memory with the given signature and timestamp.
     *
     * @param tuple
     *            the tuple to be added to the memory
     * @param signature
     *            precomputed footprint of the tuple according to the mask
     * @param timestamp
     *            the timestamp associated with the tuple
     *
     * @return A {@link Diff} describing how the timeline of the given tuple changed.
     *
     * @since 2.4
     */
    public Diff<Timestamp> addWithTimestamp(final Tuple tuple, final Tuple signature, final Timestamp timestamp) {
        throw new UnsupportedOperationException("This is only supported by timely memory implementations!");
    }

    /**
     * Adds a tuple occurrence to the memory.
     *
     * @param tuple
     *            the tuple to be added to the memory
     *
     * @return true if new signature encountered (according to the mask)
     */
    public boolean add(final Tuple tuple) {
        throw new UnsupportedOperationException("This is only supported by timeless memory implementations!");
    }

    /**
     * Adds a tuple occurrence to the memory with the given timestamp.
     *
     * @param tuple
     *            the tuple to be added to the memory
     * @param timestamp
     *            the timestamp associated with the tuple
     *
     * @return A {@link Diff} describing how the timeline of the given tuple changed.
     *
     * @since 2.4
     */
    public Diff<Timestamp> addWithTimestamp(final Tuple tuple, final Timestamp timestamp) {
        throw new UnsupportedOperationException("This is only supported by timely memory implementations!");
    }

    protected MaskedTupleMemory(final TupleMask mask, final Object owner) {
        super();
        this.mask = mask;
        this.owner = owner;
    }

    protected IllegalStateException raiseDuplicateInsertion(final Tuple tuple) {
        return new IllegalStateException(String.format("Duplicate insertion of tuple %s into %s", tuple, owner));
    }

    protected IllegalStateException raiseDuplicateDeletion(final Tuple tuple) {
        return new IllegalStateException(String.format("Duplicate deletion of tuple %s from %s", tuple, owner));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + mask + ">@" + owner;
    }

}
