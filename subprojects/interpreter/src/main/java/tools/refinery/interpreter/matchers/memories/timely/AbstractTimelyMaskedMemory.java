/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.memories.timely;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import tools.refinery.interpreter.matchers.memories.MaskedTupleMemory;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Signed;
import tools.refinery.interpreter.matchers.util.TimelyMemory;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * Common parts of timely default and timely unary implementations.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @author Tamas Szabo
 * @since 2.3
 */
abstract class AbstractTimelyMaskedMemory<Timestamp extends Comparable<Timestamp>, KeyType>
        extends MaskedTupleMemory<Timestamp> {

    protected final TreeMap<Timestamp, Set<KeyType>> foldingStates;
    protected final Map<KeyType, TimelyMemory<Timestamp>> memoryMap;
    protected final boolean isLazy;

    public AbstractTimelyMaskedMemory(final TupleMask mask, final Object owner, final boolean isLazy) {
        super(mask, owner);
        this.isLazy = isLazy;
        this.memoryMap = CollectionsFactory.createMap();
        this.foldingStates = this.isLazy ? CollectionsFactory.createTreeMap() : null;
    }

    @Override
    public void initializeWith(final MaskedTupleMemory<Timestamp> other, final Timestamp defaultValue) {
        final Iterable<Tuple> signatures = other.getSignatures();
        for (final Tuple signature : signatures) {
            if (other.isTimely()) {
                final Map<Tuple, Timeline<Timestamp>> tupleMap = other.getWithTimeline(signature);
                for (final Entry<Tuple, Timeline<Timestamp>> entry : tupleMap.entrySet()) {
                    for (final Signed<Timestamp> signed : entry.getValue().asChangeSequence()) {
                        if (signed.getDirection() == Direction.DELETE) {
                            this.removeWithTimestamp(entry.getKey(), signed.getPayload());
                        } else {
                            this.addWithTimestamp(entry.getKey(), signed.getPayload());
                        }
                    }
                }
            } else {
                final Collection<Tuple> tuples = other.get(signature);
                for (final Tuple tuple : tuples) {
                    this.addWithTimestamp(tuple, defaultValue);
                }
            }
        }
    }

    public boolean isPresentAtInfinityInteral(KeyType key) {
        final TimelyMemory<Timestamp> values = this.memoryMap.get(key);
        if (values == null) {
            return false;
        } else {
            return values.getCountAtInfinity() != 0;
        }
    }

    @Override
    public void clear() {
        this.memoryMap.clear();
    }

    @Override
    public int getKeysetSize() {
        return this.memoryMap.keySet().size();
    }

    @Override
    public int getTotalSize() {
        int i = 0;
        for (final Entry<KeyType, TimelyMemory<Timestamp>> entry : this.memoryMap.entrySet()) {
            i += entry.getValue().size();
        }
        return i;
    }

    @Override
    public Iterator<Tuple> iterator() {
        return this.memoryMap.values().stream().flatMap(e -> e.keySet().stream()).iterator();
    }

    protected Collection<Tuple> getInternal(final KeyType key) {
        final TimelyMemory<Timestamp> memory = this.memoryMap.get(key);
        if (memory == null) {
            return null;
        } else {
            return memory.getTuplesAtInfinity();
        }
    }

    public Map<Tuple, Timeline<Timestamp>> getWithTimestampInternal(final KeyType key) {
        final TimelyMemory<Timestamp> memory = this.memoryMap.get(key);
        if (memory == null) {
            return null;
        } else {
            return memory.asMap();
        }
    }

    protected Diff<Timestamp> removeInternal(final KeyType key, final Tuple tuple, final Timestamp timestamp) {
        Timestamp oldResumableTimestamp = null;
        Timestamp newResumableTimestamp = null;

        final TimelyMemory<Timestamp> keyMemory = this.memoryMap.get(key);
        if (keyMemory == null) {
            throw raiseDuplicateDeletion(tuple);
        }

        if (this.isLazy) {
            oldResumableTimestamp = keyMemory.getResumableTimestamp();
        }

        Diff<Timestamp> diff = null;
        try {
            diff = keyMemory.remove(tuple, timestamp);
        } catch (final IllegalStateException e) {
            throw raiseDuplicateDeletion(tuple);
        }
        if (keyMemory.isEmpty()) {
            this.memoryMap.remove(key);
        }

        if (this.isLazy) {
            newResumableTimestamp = keyMemory.getResumableTimestamp();
            if (!Objects.equals(oldResumableTimestamp, newResumableTimestamp)) {
                unregisterFoldingState(oldResumableTimestamp, key);
                registerFoldingState(newResumableTimestamp, key);
            }
        }

        return diff;
    }

    protected Diff<Timestamp> addInternal(final KeyType key, final Tuple tuple, final Timestamp timestamp) {
        Timestamp oldResumableTimestamp = null;
        Timestamp newResumableTimestamp = null;

        final TimelyMemory<Timestamp> keyMemory = this.memoryMap.computeIfAbsent(key,
                k -> new TimelyMemory<Timestamp>(this.isLazy));

        if (this.isLazy) {
            oldResumableTimestamp = keyMemory.getResumableTimestamp();
        }

        final Diff<Timestamp> diff = keyMemory.put(tuple, timestamp);

        if (this.isLazy) {
            newResumableTimestamp = keyMemory.getResumableTimestamp();
            if (!Objects.equals(oldResumableTimestamp, newResumableTimestamp)) {
                unregisterFoldingState(oldResumableTimestamp, key);
                registerFoldingState(newResumableTimestamp, key);
            }
        }

        return diff;
    }

    @Override
    public Diff<Timestamp> removeWithTimestamp(final Tuple tuple, final Timestamp timestamp) {
        return removeWithTimestamp(tuple, null, timestamp);
    }

    @Override
    public Diff<Timestamp> addWithTimestamp(final Tuple tuple, final Timestamp timestamp) {
        return addWithTimestamp(tuple, null, timestamp);
    }

    @Override
    public boolean isTimely() {
        return true;
    }

    protected void registerFoldingState(final Timestamp timestamp, final KeyType key) {
        if (timestamp != null) {
            this.foldingStates.compute(timestamp, (k, v) -> {
                if (v == null) {
                    v = CollectionsFactory.createSet();
                }
                v.add(key);
                return v;
            });
        }
    }

    protected void unregisterFoldingState(final Timestamp timestamp, final KeyType key) {
        if (timestamp != null) {
            this.foldingStates.compute(timestamp, (k, v) -> {
                v.remove(key);
                return v.isEmpty() ? null : v;
            });
        }
    }

    @Override
    public Timestamp getResumableTimestamp() {
        if (this.foldingStates == null || this.foldingStates.isEmpty()) {
            return null;
        } else {
            return this.foldingStates.firstKey();
        }
    }

}
