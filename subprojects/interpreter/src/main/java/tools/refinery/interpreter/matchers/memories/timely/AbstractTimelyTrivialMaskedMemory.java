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

import tools.refinery.interpreter.matchers.memories.MaskedTupleMemory;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Signed;
import tools.refinery.interpreter.matchers.util.TimelyMemory;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * Common parts of timely nullary and timely identity implementations.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @author Tamas Szabo
 * @since 2.3
 */
abstract class AbstractTimelyTrivialMaskedMemory<Timestamp extends Comparable<Timestamp>> extends MaskedTupleMemory<Timestamp> {

    protected final TimelyMemory<Timestamp> memory;

    protected AbstractTimelyTrivialMaskedMemory(final TupleMask mask, final Object owner, final boolean isLazy) {
        super(mask, owner);
        this.memory = new TimelyMemory<Timestamp>(isLazy);
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
                    this.removeWithTimestamp(tuple, defaultValue);
                }
            }
        }
    }

    @Override
    public void clear() {
        this.memory.clear();
    }

    @Override
    public int getTotalSize() {
        return this.memory.size();
    }

    @Override
    public Iterator<Tuple> iterator() {
        return this.memory.keySet().iterator();
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

    @Override
    public Timestamp getResumableTimestamp() {
        return this.memory.getResumableTimestamp();
    }

}
