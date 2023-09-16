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
 * A compact timeline may cosist of an arbitrary amount of moments.
 * It is backed by an {@link ArrayList}.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public class CompactTimeline<Timestamp extends Comparable<Timestamp>> extends Timeline<Timestamp> {

    protected final List<Timestamp> elements;

    CompactTimeline() {
        this.elements = new ArrayList<>();
    }

    CompactTimeline(final Timestamp timestamp) {
        this();
        this.elements.add(timestamp);
    }

    CompactTimeline(final List<Timestamp> timestamps) {
        this.elements = new ArrayList<>(timestamps.size());
        this.elements.addAll(timestamps);
    }

    CompactTimeline(final Diff<Timestamp> diff) {
        this.elements = new ArrayList<>(diff.size());
        Direction expected = Direction.INSERT;
        for (Signed<Timestamp> signed : diff) {
            if (!expected.equals(signed.getDirection())) {
                throw new IllegalStateException(String.format("Expected direction (%s) constraint violated! %s @%s",
                        expected, diff, signed.getPayload()));
            }
            this.elements.add(signed.getPayload());
            expected = expected.opposite();
        }
    }

    @Override
    public Signed<Timestamp> getSigned(final int index) {
        final Direction direction = index % 2 == 0 ? Direction.INSERT : Direction.DELETE;
        return new Signed<>(direction, this.getUnsigned(index));
    }

    @Override
    public Timestamp getUnsigned(final int index) {
        if (this.elements.size() <= index) {
            throw new IllegalArgumentException(
                    "Timeline size (" + this.size() + ") is smaller than requested index " + index + "!");
        } else {
            return this.elements.get(index);
        }
    }

    @Override
    public int size() {
        return this.elements.size();
    }

    @Override
    public boolean isPresentAtInfinity() {
        // if it has an odd length, then it ends with "INSERT"
        return this.size() % 2 == 1;
    }

    @Override
    public Iterable<Signed<Timestamp>> asChangeSequence() {
        Iterable<Timestamp> outer = this.elements;
        return () -> {
            final Iterator<Timestamp> itr = outer.iterator();
            return new Iterator<Signed<Timestamp>>() {
                Direction direction = Direction.INSERT;

                @Override
                public boolean hasNext() {
                    return itr.hasNext();
                }

                @Override
                public Signed<Timestamp> next() {
                    final Signed<Timestamp> result = new Signed<Timestamp>(direction, itr.next());
                    direction = direction.opposite();
                    return result;
                }
            };
        };
    }

    @Override
    public boolean isEmpty() {
        return this.elements.isEmpty();
    }

}
