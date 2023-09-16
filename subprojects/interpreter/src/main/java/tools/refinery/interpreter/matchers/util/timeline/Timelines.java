/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util.timeline;

import java.util.List;

/**
 * Utility class for creating {@link Timeline}s.
 * @author Tamas Szabo
 * @since 2.4
 */
public final class Timelines<Timestamp extends Comparable<Timestamp>> {

    public static <Timestamp extends Comparable<Timestamp>> Timeline<Timestamp> createEmpty() {
        return new CompactTimeline<Timestamp>();
    }

    public static <Timestamp extends Comparable<Timestamp>> Timeline<Timestamp> createFrom(
            final Diff<Timestamp> diffs) {
        if (diffs.size() == 1) {
            return new SingletonTimeline<Timestamp>(diffs);
        } else {
            return new CompactTimeline<Timestamp>(diffs);
        }
    }

    public static <Timestamp extends Comparable<Timestamp>> Timeline<Timestamp> createFrom(
            final List<Timestamp> timestamps) {
        if (timestamps.size() == 1) {
            return new SingletonTimeline<Timestamp>(timestamps.get(0));
        } else {
            return new CompactTimeline<Timestamp>(timestamps);
        }
    }

    public static <Timestamp extends Comparable<Timestamp>> Timeline<Timestamp> createFrom(final Timestamp timestamp) {
        return new SingletonTimeline<Timestamp>(timestamp);
    }

}
