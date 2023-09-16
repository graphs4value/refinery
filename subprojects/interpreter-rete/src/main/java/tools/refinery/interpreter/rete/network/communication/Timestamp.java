/*******************************************************************************
 * Copyright (c) 2010-2018, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.matchers.util.timeline.Timelines;

/**
 * A timestamp associated with update messages in timely evaluation.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public class Timestamp implements Comparable<Timestamp>, MessageSelector {

    protected final int value;
    public static final Timestamp ZERO = new Timestamp(0);
    /**
     * @since 2.4
     */
    public static final Timeline<Timestamp> INSERT_AT_ZERO_TIMELINE = Timelines.createFrom(Timestamp.ZERO);

    public Timestamp(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public Timestamp max(final Timestamp that) {
        if (this.value >= that.value) {
            return this;
        } else {
            return that;
        }
    }

    /**
     * @since 2.4
     */
    public Timestamp min(final Timestamp that) {
        if (this.value <= that.value) {
            return this;
        } else {
            return that;
        }
    }

    @Override
    public int compareTo(final Timestamp that) {
        return this.value - that.value;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof Timestamp)) {
            return false;
        } else {
            return this.value == ((Timestamp) obj).value;
        }
    }

    @Override
    public int hashCode() {
        return this.value;
    }

    @Override
    public String toString() {
        return Integer.toString(this.value);
    }

    /**
     * A {@link Map} implementation that associates the zero timestamp with every key. There is no suppor for
     * {@link Map#entrySet()} due to performance reasons.
     *
     * @author Tamas Szabo
     */
    public static final class AllZeroMap<T> extends AbstractMap<T, Timeline<Timestamp>> {

        private final Collection<T> wrapped;

        public AllZeroMap(Set<T> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Set<Entry<T, Timeline<Timestamp>>> entrySet() {
            throw new UnsupportedOperationException("Use the combination of keySet() and get()!");
        }

        /**
         * @since 2.4
         */
        @Override
        public Timeline<Timestamp> get(final Object key) {
            return INSERT_AT_ZERO_TIMELINE;
        }

        @Override
        public Set<T> keySet() {
            return (Set<T>) this.wrapped;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ": " + this.keySet().toString();
        }

    }

}
