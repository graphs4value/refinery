/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
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
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.TimelyMemory;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * Default timely implementation that covers all cases.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public final class TimelyDefaultMaskedTupleMemory<Timestamp extends Comparable<Timestamp>>
        extends AbstractTimelyMaskedMemory<Timestamp, Tuple> {

    public TimelyDefaultMaskedTupleMemory(final TupleMask mask, final Object owner, final boolean isLazy) {
        super(mask, owner, isLazy);
    }

    @Override
    public Iterable<Tuple> getSignatures() {
        return this.memoryMap.keySet();
    }

    @Override
    public Diff<Timestamp> removeWithTimestamp(final Tuple tuple, final Tuple signature,
            final Timestamp timestamp) {
        final Tuple key = mask.transform(tuple);
        return removeInternal(key, tuple, timestamp);
    }

    @Override
    public Diff<Timestamp> addWithTimestamp(final Tuple tuple, final Tuple signature,
            final Timestamp timestamp) {
        final Tuple key = this.mask.transform(tuple);
        return addInternal(key, tuple, timestamp);
    }

    @Override
    public Collection<Tuple> get(final ITuple signature) {
        return getInternal(signature.toImmutable());
    }

    @Override
    public Map<Tuple, Timeline<Timestamp>> getWithTimeline(final ITuple signature) {
        return getWithTimestampInternal(signature.toImmutable());
    }

    @Override
    public boolean isPresentAtInfinity(final ITuple signature) {
        return isPresentAtInfinityInteral(signature.toImmutable());
    }

    @Override
    public Set<Tuple> getResumableSignatures() {
        if (this.foldingStates == null || this.foldingStates.isEmpty()) {
            return Collections.emptySet();
        } else {
            return this.foldingStates.firstEntry().getValue();
        }
    }

    @Override
    public Map<Tuple, Map<Tuple, Diff<Timestamp>>> resumeAt(final Timestamp timestamp) {
        final Map<Tuple, Map<Tuple, Diff<Timestamp>>> result = CollectionsFactory.createMap();
        final Timestamp resumableTimestamp = this.getResumableTimestamp();
        if (resumableTimestamp == null || resumableTimestamp.compareTo(timestamp) != 0) {
            throw new IllegalStateException("Expected to continue folding at " + resumableTimestamp + "!");
        }
        final Set<Tuple> signatures = this.foldingStates.remove(timestamp);
        for (final Tuple signature : signatures) {
            final TimelyMemory<Timestamp> memory = this.memoryMap.get(signature);
            final Map<Tuple, Diff<Timestamp>> diffMap = memory.resumeAt(resumableTimestamp);
            result.put(signature, diffMap);
            registerFoldingState(memory.getResumableTimestamp(), signature);
        }
        return result;
    }

}
