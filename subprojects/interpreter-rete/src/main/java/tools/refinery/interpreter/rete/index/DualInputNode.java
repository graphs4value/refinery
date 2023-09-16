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
import java.util.Set;

import tools.refinery.interpreter.rete.network.NetworkStructureChangeSensitiveNode;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.StandardNode;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.network.delayed.DelayedConnectCommand;
import tools.refinery.interpreter.rete.traceability.TraceInfo;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.rete.util.Options;

/**
 * Abstract superclass for nodes with two inputs that are matched against each other.
 *
 * @author Gabor Bergmann
 */
public abstract class DualInputNode extends StandardNode implements NetworkStructureChangeSensitiveNode {

    /**
     * @since 2.3
     */
    protected NetworkStructureChangeSensitiveLogic logic;

    public IterableIndexer getPrimarySlot() {
        return primarySlot;
    }

    public Indexer getSecondarySlot() {
        return secondarySlot;
    }

    /**
     * @author Gabor Bergmann
     *
     */
    public enum Side {
        PRIMARY, SECONDARY, BOTH;

        public Side opposite() {
            switch (this) {
            case PRIMARY:
                return SECONDARY;
            case SECONDARY:
                return PRIMARY;
            case BOTH:
                return BOTH;
            default:
                return BOTH;
            }
        }
    }

    /**
     * Holds the primary input slot of this node.
     */
    protected IterableIndexer primarySlot;

    /**
     * Holds the secondary input slot of this node.
     */
    protected Indexer secondarySlot;

    /**
     * Optional complementer mask
     */
    protected TupleMask complementerSecondaryMask;

    /**
     * true if the primary and secondary slots coincide
     */
    protected boolean coincidence;

    /**
     * @param reteContainer
     */
    public DualInputNode(final ReteContainer reteContainer, final TupleMask complementerSecondaryMask) {
        super(reteContainer);
        this.complementerSecondaryMask = complementerSecondaryMask;
        this.indexerGroupCache = CollectionsFactory.createMap();
        this.refreshIndexerGroupCache();
    }

    /**
     * Should be called only once, when node is initialized.
     */
    public void connectToIndexers(final IterableIndexer primarySlot, final Indexer secondarySlot) {
        this.primarySlot = primarySlot;
        this.secondarySlot = secondarySlot;

        reteContainer.getCommunicationTracker().registerDependency(primarySlot, this);
        reteContainer.getCommunicationTracker().registerDependency(secondarySlot, this);

        // attach listeners
        // if there is syncing, do this after the flush done for pulling, but before syncing updates
        coincidence = primarySlot.equals(secondarySlot);

        if (!coincidence) { // regular case
            primarySlot.attachListener(new DefaultIndexerListener(this) {
                @Override
                public void notifyIndexerUpdate(final Direction direction, final Tuple updateElement,
                        final Tuple signature, final boolean change, final Timestamp timestamp) {
                    DualInputNode.this.logic.notifyUpdate(Side.PRIMARY, direction, updateElement, signature, change,
                            timestamp);
                }

                @Override
                public String toString() {
                    return "primary@" + DualInputNode.this;
                }
            });
            secondarySlot.attachListener(new DefaultIndexerListener(this) {
                public void notifyIndexerUpdate(final Direction direction, final Tuple updateElement,
                        final Tuple signature, final boolean change, final Timestamp timestamp) {
                    DualInputNode.this.logic.notifyUpdate(Side.SECONDARY, direction, updateElement, signature, change,
                            timestamp);
                }

                @Override
                public String toString() {
                    return "secondary@" + DualInputNode.this;
                }
            });
        } else { // if the two slots are the same, updates have to be handled carefully
            primarySlot.attachListener(new DefaultIndexerListener(this) {
                public void notifyIndexerUpdate(final Direction direction, final Tuple updateElement,
                        final Tuple signature, final boolean change, final Timestamp timestamp) {
                    DualInputNode.this.logic.notifyUpdate(Side.BOTH, direction, updateElement, signature, change,
                            timestamp);
                }

                @Override
                public String toString() {
                    return "both@" + DualInputNode.this;
                }
            });
        }

        for (final Receiver receiver : getReceivers()) {
            this.reteContainer.getDelayedCommandQueue()
                    .add(new DelayedConnectCommand(this, receiver, this.reteContainer));
        }

        // Given that connectToIndexers registers new dependencies, the networkStructureChanged() method will be called
        // by the CommunicationTracker, and the implementation of that method in turn will call refreshIndexerGroupCache() anyway.
        this.refreshIndexerGroupCache();
    }

    /**
     * Helper: retrieves all stored substitutions from the opposite side memory.
     *
     * @return the collection of opposite substitutions if any, or null if none
     */
    protected Collection<Tuple> retrieveOpposites(final Side side, final Tuple signature) {
        return getSlot(side.opposite()).get(signature);
    }

    /**
     * @since 2.3
     */
    protected NetworkStructureChangeSensitiveLogic createLogic() {
        if (this.reteContainer.isTimelyEvaluation()
                && this.reteContainer.getCommunicationTracker().isInRecursiveGroup(this)) {
            return createTimelyLogic();
        } else {
            return createTimelessLogic();
        }
    }

    /**
     * Helper: unifies a left and right partial matching.
     */
    protected Tuple unify(final Tuple left, final Tuple right) {
        return complementerSecondaryMask.combine(left, right, Options.enableInheritance, true);
    }

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
        this.logic.pullInto(collector, flush);
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        this.logic.pullIntoWithTimeline(collector, flush);
    }

    /**
     * Helper: unifies a substitution from the specified side with another substitution from the other side.
     */
    protected Tuple unify(final Side side, final Tuple ps, final Tuple opposite) {
        switch (side) {
        case PRIMARY:
            return unify(ps, opposite);
        case SECONDARY:
            return unify(opposite, ps);
        case BOTH:
            return unify(ps, opposite);
        default:
            return null;
        }
    }

    /**
     * Simulates the behavior of the node for calibration purposes only.
     */
    public abstract Tuple calibrate(final Tuple primary, final Tuple secondary);

    /**
     * @param complementerSecondaryMask
     *            the complementerSecondaryMask to set
     */
    public void setComplementerSecondaryMask(final TupleMask complementerSecondaryMask) {
        this.complementerSecondaryMask = complementerSecondaryMask;
    }

    /**
     * Retrieves the slot corresponding to the specified side.
     */
    protected Indexer getSlot(final Side side) {
        if (side == Side.SECONDARY) {
            return secondarySlot;
        } else {
            return primarySlot;
        }
    }

    @Override
    public void assignTraceInfo(final TraceInfo traceInfo) {
        super.assignTraceInfo(traceInfo);
        if (traceInfo.propagateToIndexerParent()) {
            if (primarySlot != null) {
                primarySlot.acceptPropagatedTraceInfo(traceInfo);
            }
            if (secondarySlot != null) {
                secondarySlot.acceptPropagatedTraceInfo(traceInfo);
            }
        }
    }

    @Override
    public void networkStructureChanged() {
        super.networkStructureChanged();
        this.logic = createLogic();
        this.refreshIndexerGroupCache();
    }

    /**
     * @since 2.3
     */
    protected abstract NetworkStructureChangeSensitiveLogic createTimelyLogic();

    /**
     * @since 2.3
     */
    protected abstract NetworkStructureChangeSensitiveLogic createTimelessLogic();

    /**
     * This map caches the result of a CommunicationTracker.areInSameGroup(indexer, this) call. It does that for both
     * the primary and secondary slots. This way we can avoid the lookup in the getWithTimestamp call for each tuple.
     * The cache needs to be maintained when the network structure changes.
     * @since 2.3
     */
    protected Map<Indexer, Boolean> indexerGroupCache;

    /**
     * @since 2.3
     */
    protected void refreshIndexerGroupCache() {
        this.indexerGroupCache.clear();
        if (this.primarySlot != null) {
            this.indexerGroupCache.put(this.primarySlot,
                    this.reteContainer.getCommunicationTracker().areInSameGroup(this.primarySlot, this));
        }
        if (this.secondarySlot != null) {
            this.indexerGroupCache.put(this.secondarySlot,
                    this.reteContainer.getCommunicationTracker().areInSameGroup(this.secondarySlot, this));
        }
    }

    /**
     * @since 2.4
     */
    protected Map<Tuple, Timeline<Timestamp>> getTimeline(final Tuple signature, final Indexer indexer) {
        if (this.indexerGroupCache.get(indexer)) {
            // recursive timely case
            return indexer.getTimeline(signature);
        } else {
            // the indexer is in a different group, treat all of its tuples as they would have timestamp 0
            final Collection<Tuple> tuples = indexer.get(signature);
            if (tuples == null) {
                return null;
            } else {
                return new Timestamp.AllZeroMap<Tuple>((Set<Tuple>) tuples);
            }
        }
    }

    /**
     * @since 2.3
     */
    protected static abstract class NetworkStructureChangeSensitiveLogic {

        /**
         * Abstract handler for update event.
         *
         * @param side
         *            The side on which the event occurred.
         * @param direction
         *            The direction of the update.
         * @param updateElement
         *            The partial matching that is inserted.
         * @param signature
         *            Masked signature of updateElement.
         * @param change
         *            Indicates whether this is/was the first/last instance of this signature in this slot.
         * @since 2.4
         */
        public abstract void notifyUpdate(final Side side, final Direction direction, final Tuple updateElement,
                final Tuple signature, final boolean change, final Timestamp timestamp);

        public abstract void pullInto(final Collection<Tuple> collector, final boolean flush);

        /**
         * @since 2.4
         */
        public abstract void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush);

    }

}
