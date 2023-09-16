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
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * Timely specialization for nullary mask.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public final class TimelyNullaryMaskedTupleMemory<Timestamp extends Comparable<Timestamp>>
        extends AbstractTimelyTrivialMaskedMemory<Timestamp> {

    protected static final Tuple EMPTY_TUPLE = Tuples.staticArityFlatTupleOf();
    protected static final Set<Tuple> UNIT_RELATION = Collections.singleton(EMPTY_TUPLE);
    protected static final Set<Tuple> EMPTY_RELATION = Collections.emptySet();

    public TimelyNullaryMaskedTupleMemory(final TupleMask mask, final Object owner, final boolean isLazy) {
        super(mask, owner, isLazy);
        if (0 != mask.getSize()) {
            throw new IllegalArgumentException(mask.toString());
        }
    }

    @Override
    public int getKeysetSize() {
        return this.memory.isEmpty() ? 0 : 1;
    }

    @Override
    public Iterable<Tuple> getSignatures() {
        return this.memory.isEmpty() ? EMPTY_RELATION : UNIT_RELATION;
    }

    @Override
    public Collection<Tuple> get(final ITuple signature) {
        if (0 == signature.getSize()) {
            return this.memory.getTuplesAtInfinity();
        } else {
            return null;
        }
    }

    @Override
    public Map<Tuple, Timeline<Timestamp>> getWithTimeline(final ITuple signature) {
        if (0 == signature.getSize()) {
            return this.memory.asMap();
        } else {
            return null;
        }
    }

    @Override
    public Diff<Timestamp> removeWithTimestamp(final Tuple tuple, final Tuple signature, final Timestamp timestamp) {
        try {
            return this.memory.remove(tuple, timestamp);
        } catch (final IllegalStateException e) {
            throw raiseDuplicateDeletion(tuple);
        }
    }

    @Override
    public Diff<Timestamp> addWithTimestamp(final Tuple tuple, final Tuple signature, final Timestamp timestamp) {
        return this.memory.put(tuple, timestamp);
    }

    @Override
    public boolean isPresentAtInfinity(final ITuple signature) {
        if (0 == signature.getSize()) {
            return this.memory.getCountAtInfinity() > 0;
        } else {
            return false;
        }
    }

    @Override
    public Set<Tuple> getResumableSignatures() {
        if (this.memory.getResumableTimestamp() != null) {
            return UNIT_RELATION;
        } else {
            return EMPTY_RELATION;
        }
    }

    @Override
    public Map<Tuple, Map<Tuple, Diff<Timestamp>>> resumeAt(final Timestamp timestamp) {
        return Collections.singletonMap(EMPTY_TUPLE, this.memory.resumeAt(timestamp));
    }

}
