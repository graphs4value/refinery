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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.TimelyMemory;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * Timely specialization for unary mask.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public final class TimelyUnaryMaskedTupleMemory<Timestamp extends Comparable<Timestamp>>
        extends AbstractTimelyMaskedMemory<Timestamp, Object> {

    protected final int keyPosition;

    public TimelyUnaryMaskedTupleMemory(final TupleMask mask, final Object owner, final boolean isLazy) {
        super(mask, owner, isLazy);
        if (1 != mask.getSize())
            throw new IllegalArgumentException(mask.toString());
        this.keyPosition = mask.indices[0];
    }

    @Override
    public Iterable<Tuple> getSignatures() {
        return () -> {
            final Iterator<Object> wrapped = this.memoryMap.keySet().iterator();
            return new Iterator<Tuple>() {
                @Override
                public boolean hasNext() {
                    return wrapped.hasNext();
                }

                @Override
                public Tuple next() {
                    final Object key = wrapped.next();
                    return Tuples.staticArityFlatTupleOf(key);
                }
            };
        };
    }

    @Override
    public Diff<Timestamp> removeWithTimestamp(final Tuple tuple, final Tuple signature, final Timestamp timestamp) {
        final Object key = tuple.get(keyPosition);
        return removeInternal(key, tuple, timestamp);
    }

    @Override
    public Diff<Timestamp> addWithTimestamp(final Tuple tuple, final Tuple signature, final Timestamp timestamp) {
        final Object key = tuple.get(keyPosition);
        return addInternal(key, tuple, timestamp);
    }

    @Override
    public Collection<Tuple> get(final ITuple signature) {
        return getInternal(signature.get(0));
    }

    @Override
    public Map<Tuple, Timeline<Timestamp>> getWithTimeline(final ITuple signature) {
        return getWithTimestampInternal(signature.get(0));
    }

    @Override
    public boolean isPresentAtInfinity(ITuple signature) {
        return isPresentAtInfinityInteral(signature.get(0));
    }

    @Override
    public Iterable<Tuple> getResumableSignatures() {
        if (this.foldingStates == null || this.foldingStates.isEmpty()) {
            return Collections.emptySet();
        } else {
            return () -> {
                final Iterator<Object> wrapped = this.foldingStates.firstEntry().getValue().iterator();
                return new Iterator<Tuple>() {
                    @Override
                    public boolean hasNext() {
                        return wrapped.hasNext();
                    }

                    @Override
                    public Tuple next() {
                        final Object key = wrapped.next();
                        return Tuples.staticArityFlatTupleOf(key);
                    }
                };
            };
        }
    }

    @Override
    public Map<Tuple, Map<Tuple, Diff<Timestamp>>> resumeAt(final Timestamp timestamp) {
        final Map<Tuple, Map<Tuple, Diff<Timestamp>>> result = CollectionsFactory.createMap();
        final Timestamp resumableTimestamp = this.getResumableTimestamp();
        if (resumableTimestamp == null) {
            throw new IllegalStateException("There is nothing to fold!");
        } else if (resumableTimestamp.compareTo(timestamp) != 0) {
            throw new IllegalStateException("Expected to continue folding at " + resumableTimestamp + "!");
        }

        final Set<Object> signatures = this.foldingStates.remove(timestamp);
        for (final Object signature : signatures) {
            final TimelyMemory<Timestamp> memory = this.memoryMap.get(signature);
            final Map<Tuple, Diff<Timestamp>> diffMap = memory.resumeAt(resumableTimestamp);
            result.put(Tuples.staticArityFlatTupleOf(signature), diffMap);
            registerFoldingState(memory.getResumableTimestamp(), signature);
        }
        return result;
    }

}
