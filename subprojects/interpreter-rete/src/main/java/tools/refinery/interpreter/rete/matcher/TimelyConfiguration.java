/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.matcher;

/**
 * Configuration of timely evaluation.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public class TimelyConfiguration {

    private final AggregatorArchitecture aggregatorArchitecture;
    private final TimelineRepresentation timelineRepresentation;

    public TimelyConfiguration(final TimelineRepresentation timelineRepresentation,
            final AggregatorArchitecture aggregatorArchitecture) {
        this.aggregatorArchitecture = aggregatorArchitecture;
        this.timelineRepresentation = timelineRepresentation;
    }

    public AggregatorArchitecture getAggregatorArchitecture() {
        return aggregatorArchitecture;
    }

    public TimelineRepresentation getTimelineRepresentation() {
        return timelineRepresentation;
    }

    public enum AggregatorArchitecture {
        /**
         * Aggregands are copied over from lower timestamps to higher timestamps.
         */
        PARALLEL,

        /**
         * Aggregands are only present at the timestamp where they are inserted at.
         * Only aggregate results are pushed towards higher timestamps during folding.
         */
        SEQUENTIAL
    }

    public enum TimelineRepresentation {
        /**
         * Only the first moment (timestamp) of appearance is maintained per tuple.
         */
        FIRST_ONLY,

        /**
         * Complete timeline (series of appearance & disappearance) is maintained per tuple.
         */
        FAITHFUL
    }

}
