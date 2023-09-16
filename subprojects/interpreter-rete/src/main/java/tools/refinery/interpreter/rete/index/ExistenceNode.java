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
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Signed;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;

/**
 * Propagates all substitutions arriving at the PRIMARY slot if and only if (a matching substitution on the SECONDARY is
 * present) xor (NEGATIVE).
 *
 * The negative parameter specifies whether this node checks for existence or non-existence.
 * <p>
 * It is mandatory in differential dataflow evaluation that the secondary parent is in an upstream dependency component
 * (so that every secondary tuple comes with zero timestamp).
 *
 * @author Gabor Bergmann
 */
public class ExistenceNode extends DualInputNode {

    protected boolean negative;

    /**
     * @param reteContainer
     * @param negative
     *            if false, act as existence checker, otherwise a nonexistence-checker
     */
    public ExistenceNode(final ReteContainer reteContainer, final boolean negative) {
        super(reteContainer, null);
        this.negative = negative;
        this.logic = createLogic();
    }

    @Override
    public Tuple calibrate(final Tuple primary, final Tuple secondary) {
        return primary;
    }

    @Override
    public void networkStructureChanged() {
        if (this.reteContainer.isTimelyEvaluation() && this.secondarySlot != null
                && this.reteContainer.getCommunicationTracker().areInSameGroup(this, this.secondarySlot)) {
            throw new IllegalStateException("Secondary parent must be in an upstream dependency component!");
        }
        super.networkStructureChanged();
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
                if ((opposites != null) ^ negative) {
                    collector.addAll(primaries);
                }
            }
        }

        @Override
        public void notifyUpdate(final Side side, final Direction direction, final Tuple updateElement,
                final Tuple signature, final boolean change, final Timestamp timestamp) {
            // in the default case, all timestamps must be zero
            assert Timestamp.ZERO.equals(timestamp);

            switch (side) {
            case PRIMARY:
                if ((retrieveOpposites(side, signature) != null) ^ negative) {
                    propagateUpdate(direction, updateElement, timestamp);
                }
                break;
            case SECONDARY:
                if (change) {
                    final Collection<Tuple> opposites = retrieveOpposites(side, signature);
                    if (opposites != null) {
                        for (final Tuple opposite : opposites) {
                            propagateUpdate((negative ? direction.opposite() : direction), opposite, timestamp);
                        }
                    }
                }
                break;
            case BOTH:
                // in case the slots coincide,
                // negative --> always empty
                // !positive --> identity
                if (!negative) {
                    propagateUpdate(direction, updateElement, timestamp);
                }
                break;
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
                // see contract: secondary must be in an upstream SCC
                final Collection<Tuple> opposites = secondarySlot.get(signature);
                if ((opposites != null) ^ negative) {
                    for (final Tuple primary : primaries.keySet()) {
                        collector.put(primary, primaries.get(primary));
                    }
                }
            }
        }

        @Override
        public void pullInto(final Collection<Tuple> collector, final boolean flush) {
            ExistenceNode.this.TIMELESS.pullInto(collector, flush);
        }

        @Override
        public void notifyUpdate(final Side side, final Direction direction, final Tuple updateElement,
                final Tuple signature, final boolean change, final Timestamp timestamp) {
            switch (side) {
            case PRIMARY: {
                final Collection<Tuple> opposites = secondarySlot.get(signature);
                if ((opposites != null) ^ negative) {
                    propagateUpdate(direction, updateElement, timestamp);
                }
                break;
            }
            case SECONDARY: {
                final Map<Tuple, Timeline<Timestamp>> opposites = primarySlot.getTimeline(signature);
                if (change) {
                    if (opposites != null) {
                        for (final Tuple opposite : opposites.keySet()) {
                            for (final Signed<Timestamp> oppositeSigned : opposites.get(opposite).asChangeSequence()) {
                                final Direction product = direction.multiply(oppositeSigned.getDirection());
                                propagateUpdate((negative ? product.opposite() : product), opposite,
                                        oppositeSigned.getPayload());
                            }
                        }
                    }
                }
                break;
            }
            case BOTH:
                // in case the slots coincide,
                // negative --> always empty
                // positive --> identity
                if (!negative) {
                    propagateUpdate(direction, updateElement, timestamp);
                }
                break;
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
