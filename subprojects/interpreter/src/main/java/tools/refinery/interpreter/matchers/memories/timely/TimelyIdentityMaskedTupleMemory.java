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
import java.util.Map.Entry;
import java.util.Set;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * Timely specialization for identity mask.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public final class TimelyIdentityMaskedTupleMemory<Timestamp extends Comparable<Timestamp>>
        extends AbstractTimelyTrivialMaskedMemory<Timestamp> {

    public TimelyIdentityMaskedTupleMemory(final TupleMask mask, final Object owner, final boolean isLazy) {
        super(mask, owner, isLazy);
        if (!mask.isIdentity())
            throw new IllegalArgumentException(mask.toString());
    }

    @Override
    public int getKeysetSize() {
        return this.memory.size();
    }

    @Override
    public Iterable<Tuple> getSignatures() {
        return this.memory.keySet();
    }

    @Override
    public Collection<Tuple> get(final ITuple signature) {
        if (this.memory.getTuplesAtInfinity().contains(signature)) {
            return Collections.singleton(signature.toImmutable());
        } else {
            return null;
        }
    }

    @Override
    public Map<Tuple, Timeline<Timestamp>> getWithTimeline(final ITuple signature) {
        final Timeline<Timestamp> value = this.memory.get(signature);
        if (value != null) {
            return Collections.singletonMap(signature.toImmutable(), value);
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
        return this.memory.isPresentAtInfinity(signature.toImmutable());
    }

    @Override
    public Set<Tuple> getResumableSignatures() {
        if (this.memory.getResumableTimestamp() != null) {
            return this.memory.getResumableTuples();
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Map<Tuple, Map<Tuple, Diff<Timestamp>>> resumeAt(final Timestamp timestamp) {
        final Map<Tuple, Diff<Timestamp>> diffMap = this.memory.resumeAt(timestamp);
        final Map<Tuple, Map<Tuple, Diff<Timestamp>>> result = CollectionsFactory.createMap();
        for (final Entry<Tuple, Diff<Timestamp>> entry : diffMap.entrySet()) {
            result.put(entry.getKey(), Collections.singletonMap(entry.getKey(), entry.getValue()));
        }
        return result;
    }

}
