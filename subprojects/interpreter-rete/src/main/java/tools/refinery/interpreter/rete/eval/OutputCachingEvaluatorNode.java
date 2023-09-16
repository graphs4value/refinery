/*******************************************************************************
 * Copyright (c) 2010-2013, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.eval;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.interpreter.rete.matcher.TimelyConfiguration;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.communication.timely.ResumableNode;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.Clearable;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Signed;
import tools.refinery.interpreter.matchers.util.TimelyMemory;
import tools.refinery.interpreter.matchers.util.timeline.Diff;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * An evaluator node that caches the evaluation result. This node is also capable of caching the timestamps associated
 * with the result tuples if it is used in recursive differential dataflow evaluation.
 *
 * @author Bergmann Gabor
 * @author Tamas Szabo
 */
public class OutputCachingEvaluatorNode extends AbstractEvaluatorNode implements Clearable, ResumableNode {

    /**
     * @since 2.3
     */
    protected NetworkStructureChangeSensitiveLogic logic;

    /**
     * @since 2.4
     */
    protected Map<Tuple, Iterable<Tuple>> outputCache;

    /**
     * Maps input tuples to timestamps. It is wrong to map evaluation result to timestamps because the different input
     * tuples may yield the same evaluation result. This field is null as long as this node is in a non-recursive group.
     *
     * @since 2.4
     */
    protected TimelyMemory<Timestamp> memory;

    /**
     * @since 2.4
     */
    protected CommunicationGroup group;

    /**
     * @since 1.5
     */
    public OutputCachingEvaluatorNode(final ReteContainer reteContainer, final EvaluatorCore core) {
        super(reteContainer, core);
        reteContainer.registerClearable(this);
        this.outputCache = CollectionsFactory.createMap();
        this.logic = createLogic();
    }

    @Override
    public CommunicationGroup getCurrentGroup() {
        return this.group;
    }

    @Override
    public void setCurrentGroup(final CommunicationGroup group) {
        this.group = group;
    }

    @Override
    public void networkStructureChanged() {
        super.networkStructureChanged();
        this.logic = createLogic();
    }

    @Override
    public void clear() {
        this.outputCache.clear();
        if (this.memory != null) {
            this.memory.clear();
        }
    }

    /**
     * @since 2.3
     */
    protected NetworkStructureChangeSensitiveLogic createLogic() {
        if (this.reteContainer.isTimelyEvaluation()
                && this.reteContainer.getCommunicationTracker().isInRecursiveGroup(this)) {
            if (this.memory == null) {
                this.memory = new TimelyMemory<Timestamp>(reteContainer.isTimelyEvaluation() && reteContainer
                        .getTimelyConfiguration().getTimelineRepresentation() == TimelyConfiguration.TimelineRepresentation.FAITHFUL);
            }
            return TIMELY;
        } else {
            return TIMELESS;
        }
    }

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
        this.logic.pullInto(collector, flush);
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        this.logic.pullIntoWithTimeline(collector, flush);
    }

    @Override
    public void update(final Direction direction, final Tuple input, final Timestamp timestamp) {
        this.logic.update(direction, input, timestamp);
    }

    /**
     * @since 2.4
     */
    @Override
    public Timestamp getResumableTimestamp() {
        if (this.memory == null) {
            return null;
        } else {
            return this.memory.getResumableTimestamp();
        }
    }

    /**
     * @since 2.4
     */
    @Override
    public void resumeAt(final Timestamp timestamp) {
        this.logic.resumeAt(timestamp);
    }

    /**
     * @since 2.3
     */
    protected static abstract class NetworkStructureChangeSensitiveLogic {

        /**
         * @since 2.4
         */
        public abstract void update(final Direction direction, final Tuple input, final Timestamp timestamp);

        public abstract void pullInto(final Collection<Tuple> collector, final boolean flush);

        /**
         * @since 2.4
         */
        public abstract void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush);

        /**
         * @since 2.4
         */
        public abstract void resumeAt(final Timestamp timestamp);

    }

    private final NetworkStructureChangeSensitiveLogic TIMELESS = new NetworkStructureChangeSensitiveLogic() {

        @Override
        public void resumeAt(final Timestamp timestamp) {
            // there is nothing to resume in the timeless case because we do not even care about timestamps
        }

        @Override
        public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void pullInto(final Collection<Tuple> collector, final boolean flush) {
            for (final Iterable<Tuple> output : outputCache.values()) {
                if (output != NORESULT) {
                    final Iterator<Tuple> itr = output.iterator();
                    while (itr.hasNext()) {
                        collector.add(itr.next());
                    }
                }
            }
        }

        @Override
        public void update(final Direction direction, final Tuple input, final Timestamp timestamp) {
            if (direction == Direction.INSERT) {
                final Iterable<Tuple> output = core.performEvaluation(input);
                if (output != null) {
                    final Iterable<Tuple> previous = outputCache.put(input, output);
                    if (previous != null) {
                        throw new IllegalStateException(
                                String.format("Duplicate insertion of tuple %s into node %s", input, this));
                    }
                    propagateIterableUpdate(direction, output, timestamp);
                }
            } else {
                final Iterable<Tuple> output = outputCache.remove(input);
                if (output != null) {
                    // may be null if no result was yielded
                    propagateIterableUpdate(direction, output, timestamp);
                }
            }
        }
    };

    private final NetworkStructureChangeSensitiveLogic TIMELY = new NetworkStructureChangeSensitiveLogic() {

        @Override
        public void resumeAt(final Timestamp timestamp) {
            final Map<Tuple, Diff<Timestamp>> diffMap = memory.resumeAt(timestamp);

            for (final Entry<Tuple, Diff<Timestamp>> entry : diffMap.entrySet()) {
                final Tuple input = entry.getKey();
                final Iterable<Tuple> output = outputCache.get(input);
                if (output != NORESULT) {
                    for (final Signed<Timestamp> signed : entry.getValue()) {
                        propagateIterableUpdate(signed.getDirection(), output, signed.getPayload());
                    }
                }

                if (memory.get(input) == null) {
                    outputCache.remove(input);
                }
            }

            final Timestamp nextTimestamp = memory.getResumableTimestamp();
            if (nextTimestamp != null) {
                group.notifyHasMessage(mailbox, nextTimestamp);
            }
        }

        @Override
        public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
            for (final Entry<Tuple, Timeline<Timestamp>> entry : memory.asMap().entrySet()) {
                final Tuple input = entry.getKey();
                final Iterable<Tuple> output = outputCache.get(input);
                if (output != NORESULT) {
                    final Timeline<Timestamp> timestamp = entry.getValue();
                    final Iterator<Tuple> itr = output.iterator();
                    while (itr.hasNext()) {
                        collector.put(itr.next(), timestamp);
                    }
                }
            }
        }

        @Override
        public void pullInto(final Collection<Tuple> collector, final boolean flush) {
            TIMELESS.pullInto(collector, flush);
        }

        @Override
        public void update(final Direction direction, final Tuple input, final Timestamp timestamp) {
            if (direction == Direction.INSERT) {
                Iterable<Tuple> output = outputCache.get(input);
                if (output == null) {
                    output = core.performEvaluation(input);
                    if (output == null) {
                        // the evaluation result is really null
                        output = NORESULT;
                    }
                    outputCache.put(input, output);
                }
                final Diff<Timestamp> diff = memory.put(input, timestamp);
                if (output != NORESULT) {
                    for (final Signed<Timestamp> signed : diff) {
                        propagateIterableUpdate(signed.getDirection(), output, signed.getPayload());
                    }
                }
            } else {
                final Iterable<Tuple> output = outputCache.get(input);
                final Diff<Timestamp> diff = memory.remove(input, timestamp);
                if (memory.get(input) == null) {
                    outputCache.remove(input);
                }
                if (output != NORESULT) {
                    for (final Signed<Timestamp> signed : diff) {
                        propagateIterableUpdate(signed.getDirection(), output, signed.getPayload());
                    }
                }
            }
        }
    };

    /**
     * This field is used to represent the "null" evaluation result. This is an optimization used in the timely case
     * where the same tuple may be inserted multiple times with different timestamps. This way, we can also cache if
     * something evaluated to null (instead of just forgetting about the previously computed result), thus avoiding the
     * need to re-run a potentially expensive evaluation.
     */
    private static final Iterable<Tuple> NORESULT = Collections
            .singleton(Tuples.staticArityFlatTupleOf(NoResult.INSTANCE));

    private enum NoResult {
        INSTANCE
    }

}
