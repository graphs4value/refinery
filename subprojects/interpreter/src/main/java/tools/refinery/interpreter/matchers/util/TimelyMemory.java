/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.resumable.Resumable;
import tools.refinery.interpreter.matchers.util.resumable.UnmaskedResumable;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.matchers.util.timeline.Timelines;

/**
 * A timely memory implementation that incrementally maintains the {@link Timeline}s of tuples. The memory is capable of
 * lazy folding (see {@link Resumable}).
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public class TimelyMemory<Timestamp extends Comparable<Timestamp>> implements Clearable, UnmaskedResumable<Timestamp> {

    protected final Map<Tuple, TreeMap<Timestamp, CumulativeCounter>> counters;
    protected final Map<Tuple, Timeline<Timestamp>> timelines;
    public final TreeMap<Timestamp, Map<Tuple, FoldingState>> foldingState;
    protected final Set<Tuple> presentAtInfinity;
    protected final boolean isLazy;
    protected final Diff<Timestamp> EMPTY_DIFF = new Diff<Timestamp>();

    public TimelyMemory() {
        this(false);
    }

    public TimelyMemory(final boolean isLazy) {
        this.counters = CollectionsFactory.createMap();
        this.timelines = CollectionsFactory.createMap();
        this.presentAtInfinity = CollectionsFactory.createSet();
        this.isLazy = isLazy;
        if (isLazy) {
            this.foldingState = CollectionsFactory.createTreeMap();
        } else {
            this.foldingState = null;
        }
    }

    @Override
    public Set<Tuple> getResumableTuples() {
        if (this.foldingState == null || this.foldingState.isEmpty()) {
            return Collections.emptySet();
        } else {
            return this.foldingState.firstEntry().getValue().keySet();
        }
    }

    @Override
    public Timestamp getResumableTimestamp() {
        if (this.foldingState == null || this.foldingState.isEmpty()) {
            return null;
        } else {
            return this.foldingState.firstKey();
        }
    }

    /**
     * Registers the given folding state for the specified timestamp and tuple. If there is already a state stored, the
     * two states will be merged together.
     */
    protected void addFoldingState(final Tuple tuple, final FoldingState state, final Timestamp timestamp) {
        assert state.diff != 0;
        final Map<Tuple, FoldingState> tupleMap = this.foldingState.computeIfAbsent(timestamp,
                k -> CollectionsFactory.createMap());
        tupleMap.compute(tuple, (k, v) -> {
            return v == null ? state : v.merge(state);
        });
    }

    @Override
    public Map<Tuple, Diff<Timestamp>> resumeAt(final Timestamp timestamp) {
        Timestamp current = this.getResumableTimestamp();
        if (current == null) {
            throw new IllegalStateException("There is othing to fold!");
        } else if (current.compareTo(timestamp) != 0) {
            // It can happen that already registered folding states end up having zero diffs,
            // and we are instructed to continue folding at a timestamp that is higher
            // than the lowest timestamp with a folding state.
            // However, we only do garbage collection in doFoldingState, so now it is time to
            // first clean up those states with zero diffs.
            while (current != null && current.compareTo(timestamp) < 0) {
                final Map<Tuple, FoldingState> tupleMap = this.foldingState.remove(current);
                for (final Entry<Tuple, FoldingState> entry : tupleMap.entrySet()) {
                    final Tuple key = entry.getKey();
                    final FoldingState value = entry.getValue();
                    if (value.diff != 0) {
                        throw new IllegalStateException("Expected zero diff during garbage collection at " + current
                                + ", but the diff was " + value.diff + "!");
                    }
                    doFoldingStep(key, value, current);
                }
                current = this.getResumableTimestamp();
            }
            if (current == null || current.compareTo(timestamp) != 0) {
                throw new IllegalStateException("Expected to continue folding at " + timestamp + "!");
            }
        }

        final Map<Tuple, Diff<Timestamp>> diffMap = CollectionsFactory.createMap();
        final Map<Tuple, FoldingState> tupleMap = this.foldingState.remove(timestamp);
        for (final Entry<Tuple, FoldingState> entry : tupleMap.entrySet()) {
            final Tuple key = entry.getKey();
            final FoldingState value = entry.getValue();
            diffMap.put(key, doFoldingStep(key, value, timestamp));
        }

        if (this.foldingState.get(timestamp) != null) {
            throw new IllegalStateException(
                    "Folding at " + timestamp + " produced more folding work at the same timestamp!");
        }

        return diffMap;
    }

    protected Diff<Timestamp> doFoldingStep(final Tuple tuple, final FoldingState state, final Timestamp timestamp) {
        final CumulativeCounter counter = getCounter(tuple, timestamp);
        if (state.diff == 0) {
            gcCounters(counter, tuple, timestamp);
            return EMPTY_DIFF;
        } else {
            final Diff<Timestamp> resultDiff = new Diff<>();
            final Timestamp nextTimestamp = this.counters.get(tuple).higherKey(timestamp);

            final int oldCumulative = counter.cumulative;

            counter.cumulative += state.diff;

            computeDiffsLazy(state.diff < 0 ? Direction.DELETE : Direction.INSERT, oldCumulative, counter.cumulative,
                    timestamp, nextTimestamp, resultDiff);

            gcCounters(counter, tuple, timestamp);
            updateTimeline(tuple, resultDiff);

            // prepare folding state for next timestamp
            if (nextTimestamp != null) {
                // propagate the incoming diff, not the diff stored in counter
                addFoldingState(tuple, new FoldingState(state.diff), nextTimestamp);
            }

            return resultDiff;
        }
    }

    /**
     * On-demand initializes and returns the counter for the given tuple and timestamp.
     */
    protected CumulativeCounter getCounter(final Tuple tuple, final Timestamp timestamp) {
        final TreeMap<Timestamp, CumulativeCounter> counterTimeline = this.counters.computeIfAbsent(tuple,
                k -> CollectionsFactory.createTreeMap());

        final CumulativeCounter counter = counterTimeline.computeIfAbsent(timestamp, k -> {
            final Entry<Timestamp, CumulativeCounter> previousCounter = counterTimeline.lowerEntry(k);
            final int previousCumulative = previousCounter == null ? 0 : previousCounter.getValue().cumulative;
            return new CumulativeCounter(0, previousCumulative);
        });

        return counter;
    }

    /**
     * Garbage collects the counter of the given tuple and timestamp if the new diff is zero.
     */
    protected void gcCounters(final CumulativeCounter counter, final Tuple tuple, final Timestamp timestamp) {
        if (counter.diff == 0) {
            final TreeMap<Timestamp, CumulativeCounter> counterMap = this.counters.get(tuple);
            counterMap.remove(timestamp);
            if (counterMap.isEmpty()) {
                this.counters.remove(tuple);
            }
        }
    }

    /**
     * Utility method that computes the timeline diffs in case of lazy memories. The diffs will be inserted into the
     * input parameter. This method computes diffs for entire plateaus that spans from timestamp to nextTimestamp.
     *
     * Compared to the eager version of this method, the lazy version makes use of both the old and the new cumulative
     * values because it can happen that the cumulative is incremented by a value that is larger than 1 (as folding
     * states are merged together). This means that we cant decide whether the cumulative became positive by comparing
     * the new value to 1.
     */
    protected void computeDiffsLazy(final Direction direction, final int oldCumulative, final int newCumulative,
            final Timestamp timestamp, final Timestamp nextTimestamp, final Diff<Timestamp> diffs) {
        if (direction == Direction.INSERT) {
            if (newCumulative == 0) {
                throw new IllegalStateException("Cumulative count can never be negative!");
            } else {
                if (oldCumulative == 0 /* current became positive */) {
                    // (1) either we sent out a DELETE before and now we need to cancel it,
                    // (2) or we just INSERT this for the first time
                    diffs.add(new Signed<>(Direction.INSERT, timestamp));
                    if (nextTimestamp != null) {
                        diffs.add(new Signed<>(Direction.DELETE, nextTimestamp));
                    }
                } else /* current stays positive */ {
                    // nothing to do
                }
            }
        } else {
            if (newCumulative < 0) {
                throw new IllegalStateException("Cumulative count can never be negative!");
            } else {
                if (newCumulative == 0 /* current became zero */) {
                    diffs.add(new Signed<>(Direction.DELETE, timestamp));
                    if (nextTimestamp != null) {
                        diffs.add(new Signed<>(Direction.INSERT, nextTimestamp));
                    }
                } else /* current stays positive */ {
                    // nothing to do
                }
            }
        }
    }

    /**
     * Utility method that computes the timeline diffs in case of eager memories. The diffs will be inserted into the
     * input parameter. This method computes diffs that describe momentary changes instead of plateaus. Returns a
     * {@link SignChange} that describes how the sign has changed at the given timestamp.
     */
    protected SignChange computeDiffsEager(final Direction direction, final CumulativeCounter counter,
            final SignChange signChangeAtPrevious, final Timestamp timestamp, final Diff<Timestamp> diffs) {
        if (direction == Direction.INSERT) {
            if (counter.cumulative == 0) {
                throw new IllegalStateException("Cumulative count can never be negative!");
            } else {
                if (counter.cumulative == 1 /* current became positive */) {
                    if (signChangeAtPrevious != SignChange.BECAME_POSITIVE) {
                        // (1) either we sent out a DELETE before and now we need to cancel it,
                        // (2) or we just INSERT this for the first time
                        diffs.add(new Signed<>(Direction.INSERT, timestamp));
                    } else {
                        // we have already emitted this at the previous timestamp
                        // both previous and current became positive
                        throw new IllegalStateException(
                                "This would mean that the diff at current is 0 " + counter.diff);
                    }

                    // remember for next timestamp
                    return SignChange.BECAME_POSITIVE;
                } else /* current stays positive */ {
                    if (signChangeAtPrevious == SignChange.BECAME_POSITIVE) {
                        // we sent out an INSERT before and now the timeline is positive already starting at previous
                        // we need to cancel the effect of this with a DELETE
                        diffs.add(new Signed<>(Direction.DELETE, timestamp));
                    } else {
                        // this is normal, both previous and current was positive and stays positive
                    }

                    // remember for next timestamp
                    return SignChange.IRRELEVANT;
                }
            }
        } else {
            if (counter.cumulative < 0) {
                throw new IllegalStateException("Cumulative count can never be negative!");
            } else {
                if (counter.cumulative == 0 /* current became zero */) {
                    if (signChangeAtPrevious != SignChange.BECAME_ZERO) {
                        // (1) either we sent out a INSERT before and now we need to cancel it,
                        // (2) or we just DELETE this for the first time
                        diffs.add(new Signed<>(Direction.DELETE, timestamp));
                    } else {
                        // we have already emitted this at the previous timestamp
                        // both previous and current became zero
                        throw new IllegalStateException(
                                "This would mean that the diff at current is 0 " + counter.diff);
                    }

                    // remember for next timestamp
                    return SignChange.BECAME_ZERO;
                } else /* current stays positive */ {
                    if (signChangeAtPrevious == SignChange.BECAME_ZERO) {
                        // we sent out a DELETE before and now the timeline is zero already starting at previous
                        // we need to cancel the effect of this with a INSERT
                        diffs.add(new Signed<>(Direction.INSERT, timestamp));
                    } else {
                        // this is normal, both previous and current was positive and stays positive
                    }

                    // remember for next timestamp
                    return SignChange.IRRELEVANT;
                }
            }
        }
    }

    public Diff<Timestamp> put(final Tuple tuple, final Timestamp timestamp) {
        if (this.isLazy) {
            return putLazy(tuple, timestamp);
        } else {
            return putEager(tuple, timestamp);
        }
    }

    public Diff<Timestamp> remove(final Tuple tuple, final Timestamp timestamp) {
        if (this.isLazy) {
            return removeLazy(tuple, timestamp);
        } else {
            return removeEager(tuple, timestamp);
        }
    }

    protected Diff<Timestamp> putEager(final Tuple tuple, final Timestamp timestamp) {
        final Diff<Timestamp> resultDiff = new Diff<>();
        final CumulativeCounter counter = getCounter(tuple, timestamp);
        ++counter.diff;

        // before the INSERT timestamp, no change at all
        // it cannot happen that those became positive in this round
        SignChange signChangeAtPrevious = SignChange.IRRELEVANT;

        final NavigableMap<Timestamp, CumulativeCounter> nextCounters = this.counters.get(tuple).tailMap(timestamp,
                true);
        for (final Entry<Timestamp, CumulativeCounter> currentEntry : nextCounters.entrySet()) {
            final Timestamp currentTimestamp = currentEntry.getKey();
            final CumulativeCounter currentCounter = currentEntry.getValue();
            ++currentCounter.cumulative;
            signChangeAtPrevious = computeDiffsEager(Direction.INSERT, currentCounter, signChangeAtPrevious,
                    currentTimestamp, resultDiff);
        }

        gcCounters(counter, tuple, timestamp);
        updateTimeline(tuple, resultDiff);

        return resultDiff;
    }

    protected Diff<Timestamp> putLazy(final Tuple tuple, final Timestamp timestamp) {
        final CumulativeCounter counter = getCounter(tuple, timestamp);
        counter.diff += 1;
        // before the INSERT timestamp, no change at all
        // it cannot happen that those became positive in this round
        addFoldingState(tuple, new FoldingState(+1), timestamp);
        return EMPTY_DIFF;
    }

    protected Diff<Timestamp> removeEager(final Tuple tuple, final Timestamp timestamp) {
        final Diff<Timestamp> resultDiff = new Diff<>();
        final CumulativeCounter counter = getCounter(tuple, timestamp);
        --counter.diff;

        // before the DELETE timestamp, no change at all
        // it cannot happen that those became zero in this round
        SignChange signChangeAtPrevious = SignChange.IRRELEVANT;

        final NavigableMap<Timestamp, CumulativeCounter> nextCounters = this.counters.get(tuple).tailMap(timestamp,
                true);
        for (final Entry<Timestamp, CumulativeCounter> currentEntry : nextCounters.entrySet()) {
            final Timestamp currentTimestamp = currentEntry.getKey();
            final CumulativeCounter currentCounter = currentEntry.getValue();
            --currentCounter.cumulative;
            signChangeAtPrevious = computeDiffsEager(Direction.DELETE, currentCounter, signChangeAtPrevious,
                    currentTimestamp, resultDiff);
        }

        gcCounters(counter, tuple, timestamp);
        updateTimeline(tuple, resultDiff);

        return resultDiff;
    }

    protected Diff<Timestamp> removeLazy(final Tuple tuple, final Timestamp timestamp) {
        final CumulativeCounter counter = getCounter(tuple, timestamp);
        counter.diff -= 1;
        // before the DELETE timestamp, no change at all
        // it cannot happen that those became zero in this round
        addFoldingState(tuple, new FoldingState(-1), timestamp);
        return EMPTY_DIFF;
    }

    /**
     * Updates and garbage collects the timeline of the given tuple based on the given timeline diff.
     */
    protected void updateTimeline(final Tuple tuple, final Diff<Timestamp> diff) {
        if (!diff.isEmpty()) {
            this.timelines.compute(tuple, (k, oldTimeline) -> {
                this.presentAtInfinity.remove(tuple);
                final Timeline<Timestamp> timeline = oldTimeline == null ? Timelines.createFrom(diff)
                        : oldTimeline.mergeAdditive(diff);
                if (timeline.isPresentAtInfinity()) {
                    this.presentAtInfinity.add(tuple);
                }
                if (timeline.isEmpty()) {
                    return null;
                } else {
                    return timeline;
                }
            });
        }
    }

    /**
     * @since 2.8
     */
    public Set<Tuple> getTuplesAtInfinity() {
        return this.presentAtInfinity;
    }

    /**
     * Returns the number of tuples that are present at the moment 'infinity'.
     */
    public int getCountAtInfinity() {
        return this.presentAtInfinity.size();
    }

    /**
     * Returns true if the given tuple is present at the moment 'infinity'.
     */
    public boolean isPresentAtInfinity(final Tuple tuple) {
        final Timeline<Timestamp> timeline = this.timelines.get(tuple);
        if (timeline == null) {
            return false;
        } else {
            return timeline.isPresentAtInfinity();
        }
    }

    public boolean isEmpty() {
        return this.counters.isEmpty();
    }

    public int size() {
        return this.counters.size();
    }

    public Set<Tuple> keySet() {
        return this.counters.keySet();
    }

    public Map<Tuple, Timeline<Timestamp>> asMap() {
        return this.timelines;
    }

    public Timeline<Timestamp> get(final ITuple tuple) {
        return this.timelines.get(tuple);
    }

    @Override
    public void clear() {
        this.counters.clear();
        this.timelines.clear();
        if (this.foldingState != null) {
            this.foldingState.clear();
        }
    }

    public boolean containsKey(final ITuple tuple) {
        return this.counters.containsKey(tuple);
    }

    @Override
    public String toString() {
        return this.counters + "\n" + this.timelines + "\n" + this.foldingState + "\n";
    }

    protected static final class CumulativeCounter {
        protected int diff;
        protected int cumulative;

        protected CumulativeCounter(final int diff, final int cumulative) {
            this.diff = diff;
            this.cumulative = cumulative;
        }

        @Override
        public String toString() {
            return "{diff=" + this.diff + ", cumulative=" + this.cumulative + "}";
        }

    }

    protected static final class FoldingState {
        protected final int diff;

        protected FoldingState(final int diff) {
            this.diff = diff;
        }

        @Override
        public String toString() {
            return "{diff=" + this.diff + "}";
        }

        /**
         * The returned result will never be null, even if the resulting diff is zero.
         */
        public FoldingState merge(final FoldingState that) {
            Preconditions.checkArgument(that != null);
            return new FoldingState(this.diff + that.diff);
        }

    }

    protected enum SignChange {
        BECAME_POSITIVE, BECAME_ZERO, IRRELEVANT;
    }

}
