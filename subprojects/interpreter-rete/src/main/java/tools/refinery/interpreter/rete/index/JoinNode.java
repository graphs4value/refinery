/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.index;

import java.util.Collection;
import java.util.Map;

import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Signed;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * @author Gabor Bergmann
 *
 */
public class JoinNode extends DualInputNode {

    public JoinNode(final ReteContainer reteContainer, final TupleMask complementerSecondaryMask) {
        super(reteContainer, complementerSecondaryMask);
        this.logic = createLogic();
    }

    @Override
    public Tuple calibrate(final Tuple primary, final Tuple secondary) {
        return unify(primary, secondary);
    }

    private final NetworkStructureChangeSensitiveLogic TIMELESS = new NetworkStructureChangeSensitiveLogic() {

        @Override
        public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void pullInto(final Collection<Tuple> collector, final boolean flush) {
            if (primarySlot == null || secondarySlot == null) {
                return;
            }

            if (flush) {
                reteContainer.flushUpdates();
            }

            for (final Tuple signature : primarySlot.getSignatures()) {
                // primaries can not be null due to the contract of IterableIndex.getSignatures()
                final Collection<Tuple> primaries = primarySlot.get(signature);
                final Collection<Tuple> opposites = secondarySlot.get(signature);
                if (opposites != null) {
                    for (final Tuple primary : primaries) {
                        for (final Tuple opposite : opposites) {
                            collector.add(unify(primary, opposite));
                        }
                    }
                }
            }
        }

        @Override
        public void notifyUpdate(final Side side, final Direction direction, final Tuple updateElement,
                final Tuple signature, final boolean change, final Timestamp timestamp) {
            // in the default case, all timestamps must be zero
            assert Timestamp.ZERO.equals(timestamp);

            final Collection<Tuple> opposites = retrieveOpposites(side, signature);

            if (!coincidence) {
                if (opposites != null) {
                    for (final Tuple opposite : opposites) {
                        propagateUpdate(direction, unify(side, updateElement, opposite), timestamp);
                    }
                }
            } else {
                // compensate for coincidence of slots - this is the case when an Indexer is joined with itself
                if (opposites != null) {
                    for (final Tuple opposite : opposites) {
                        if (opposite.equals(updateElement)) {
                            // handle self-joins of a single tuple separately
                            continue;
                        }
                        propagateUpdate(direction, unify(opposite, updateElement), timestamp);
                        propagateUpdate(direction, unify(updateElement, opposite), timestamp);
                    }
                }

                // handle self-joins here
                propagateUpdate(direction, unify(updateElement, updateElement), timestamp);
            }
        }
    };

    private final NetworkStructureChangeSensitiveLogic TIMELY = new NetworkStructureChangeSensitiveLogic() {

        @Override
        public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
            if (primarySlot == null || secondarySlot == null) {
                return;
            }

            if (flush) {
                reteContainer.flushUpdates();
            }

            for (final Tuple signature : primarySlot.getSignatures()) {
                // primaries can not be null due to the contract of IterableIndex.getSignatures()
                final Map<Tuple, Timeline<Timestamp>> primaries = getTimeline(signature, primarySlot);
                final Map<Tuple, Timeline<Timestamp>> opposites = getTimeline(signature, secondarySlot);
                if (opposites != null) {
                    for (final Tuple primary : primaries.keySet()) {
                        for (final Tuple opposite : opposites.keySet()) {
                            final Timeline<Timestamp> primaryTimeline = primaries.get(primary);
                            final Timeline<Timestamp> oppositeTimeline = opposites.get(opposite);
                            final Timeline<Timestamp> mergedTimeline = primaryTimeline
                                    .mergeMultiplicative(oppositeTimeline);
                            if (!mergedTimeline.isEmpty()) {
                                collector.put(unify(primary, opposite), mergedTimeline);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void pullInto(final Collection<Tuple> collector, final boolean flush) {
            JoinNode.this.TIMELESS.pullInto(collector, flush);
        }

        @Override
        public void notifyUpdate(final Side side, final Direction direction, final Tuple updateElement,
                final Tuple signature, final boolean change, final Timestamp timestamp) {
            final Indexer oppositeIndexer = getSlot(side.opposite());
            final Map<Tuple, Timeline<Timestamp>> opposites = getTimeline(signature, oppositeIndexer);

            if (!coincidence) {
                if (opposites != null) {
                    for (final Tuple opposite : opposites.keySet()) {
                        final Tuple unifiedTuple = unify(side, updateElement, opposite);
                        for (final Signed<Timestamp> signed : opposites.get(opposite).asChangeSequence()) {
                            // TODO only consider signed timestamps that are greater or equal to timestamp
                            // plus compact the previous timestamps into at most one update
                            propagateUpdate(signed.getDirection().multiply(direction), unifiedTuple,
                                    timestamp.max(signed.getPayload()));
                        }
                    }
                }
            } else {
                // compensate for coincidence of slots - this is the case when an Indexer is joined with itself
                if (opposites != null) {
                    for (final Tuple opposite : opposites.keySet()) {
                        if (opposite.equals(updateElement)) {
                            // handle self-joins of a single tuple separately
                            continue;
                        }
                        final Tuple u1 = unify(opposite, updateElement);
                        final Tuple u2 = unify(updateElement, opposite);
                        for (final Signed<Timestamp> oppositeSigned : opposites.get(opposite).asChangeSequence()) {
                            final Direction updateDirection = direction.multiply(oppositeSigned.getDirection());
                            final Timestamp updateTimestamp = timestamp.max(oppositeSigned.getPayload());
                            propagateUpdate(updateDirection, u1, updateTimestamp);
                            propagateUpdate(updateDirection, u2, updateTimestamp);
                        }
                    }
                }

                // handle self-join here
                propagateUpdate(direction, unify(updateElement, updateElement), timestamp);
            }
        }
    };

    @Override
    protected NetworkStructureChangeSensitiveLogic createTimelessLogic() {
        return this.TIMELESS;
    }

    @Override
    protected NetworkStructureChangeSensitiveLogic createTimelyLogic() {
        return this.TIMELY;
    }

}
