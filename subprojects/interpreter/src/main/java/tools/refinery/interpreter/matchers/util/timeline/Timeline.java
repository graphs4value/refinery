/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util.timeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Signed;

/**
 * A timeline describes the life cycle of a piece of data (typically a tuple in a relation) as a sequence of moments.
 * Even moments represent appearances, odd moments represent disappearances. A timeline is immutable, once created, it
 * is not possible to extend it with further moments.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public abstract class Timeline<Timestamp extends Comparable<Timestamp>> {

    public abstract Iterable<Signed<Timestamp>> asChangeSequence();

    public abstract boolean isPresentAtInfinity();

    public abstract boolean isEmpty();

    public abstract int size();

    public abstract Signed<Timestamp> getSigned(final int index);

    public abstract Timestamp getUnsigned(final int index);

    public Timeline<Timestamp> mergeMultiplicative(final Timeline<Timestamp> that) {
        final List<Timestamp> result = new ArrayList<>();
        int thisIdx = 0, thatIdx = 0;
        Timestamp thisNext = thisIdx < this.size() ? this.getUnsigned(thisIdx) : null;
        Timestamp thatNext = thatIdx < that.size() ? that.getUnsigned(thatIdx) : null;

        while (thisNext != null || thatNext != null) {
            int thisMinusThat = 0;
            if (thisNext != null && thatNext != null) {
                thisMinusThat = thisNext.compareTo(thatNext);
            }
            if (thisNext == null || thisMinusThat > 0) {
                if (thisIdx % 2 == 1) {
                    result.add(thatNext);
                }
                thatIdx++;
                thatNext = thatIdx < that.size() ? that.getUnsigned(thatIdx) : null;
            } else if (thatNext == null || thisMinusThat < 0) {
                if (thatIdx % 2 == 1) {
                    result.add(thisNext);
                }
                thisIdx++;
                thisNext = thisIdx < this.size() ? this.getUnsigned(thisIdx) : null;
            } else {
                if (thisIdx % 2 == thatIdx % 2) {
                    result.add(thisNext);
                }
                thisIdx++;
                thatIdx++;
                thatNext = thatIdx < that.size() ? that.getUnsigned(thatIdx) : null;
                thisNext = thisIdx < this.size() ? this.getUnsigned(thisIdx) : null;
            }
        }

        return Timelines.createFrom(result);
    }

    /**
     * Merges this timeline with the given timestamp diff. The expectation is that the resulting timeline starts with an
     * insertion. The logic is similar to a merge sort; we iterate side-by-side over the timeline and the diff. During
     * the merge, cancellation can happen if at the same timestamp we observe different signs at the corresponding
     * timeline and diff elements.
     */
    public Timeline<Timestamp> mergeAdditive(final Diff<Timestamp> diff) {
        final Iterator<Signed<Timestamp>> thisItr = this.asChangeSequence().iterator();
        final Iterator<Signed<Timestamp>> diffItr = diff.iterator();
        final List<Timestamp> result = new ArrayList<>();
        Direction expected = Direction.INSERT;
        Signed<Timestamp> thisNext = thisItr.hasNext() ? thisItr.next() : null;
        Signed<Timestamp> diffNext = diffItr.hasNext() ? diffItr.next() : null;

        while (thisNext != null || diffNext != null) {
            int thisMinusDiff = 0;
            if (thisNext != null && diffNext != null) {
                thisMinusDiff = thisNext.getPayload().compareTo(diffNext.getPayload());
            }

            if (thisNext == null || thisMinusDiff > 0) {
                if (!expected.equals(diffNext.getDirection())) {
                    throw new IllegalStateException(
                            String.format("Expected direction (%s) constraint violated! %s %s @%s", expected, this,
                                    diff, diffNext.getPayload()));
                }
                result.add(diffNext.getPayload());
                diffNext = diffItr.hasNext() ? diffItr.next() : null;
                expected = expected.opposite();
            } else if (diffNext == null || thisMinusDiff < 0) {
                if (!expected.equals(thisNext.getDirection())) {
                    throw new IllegalStateException(
                            String.format("Expected direction (%s) constraint violated! %s %s @%s", expected, this,
                                    diff, thisNext.getPayload()));
                }
                result.add(thisNext.getPayload());
                thisNext = thisItr.hasNext() ? thisItr.next() : null;
                expected = expected.opposite();
            } else {
                // they cancel out each other
                if (diffNext.getDirection().equals(thisNext.getDirection())) {
                    throw new IllegalStateException(String.format("Changes do not cancel out each other! %s %s @%s",
                            this, diff, thisNext.getPayload()));
                }
                diffNext = diffItr.hasNext() ? diffItr.next() : null;
                thisNext = thisItr.hasNext() ? thisItr.next() : null;
            }
        }

        return Timelines.createFrom(result);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        boolean first = true;
        for (final Signed<Timestamp> element : this.asChangeSequence()) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(element.toString());
        }
        builder.append("]");
        return builder.toString();
    }

}
